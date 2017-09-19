package org.citopt.connde.conndeapp.advertise;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;

/**
 * Created by rosso on 19.08.17.
 */

public abstract class AdvertiseClient {
  private static final Logger log = LoggerFactory.getLogger(AdvertiseClient.class);
  private InetAddress server_address;
  private InetAddress ip;
  private String hw_addr;
  private AdvertiseService service;
  private String comm_type;

  public AdvertiseClient(String comm_type, AdvertiseService service) {
    this.service = service;
    this.comm_type = comm_type;
  }

  protected abstract void _send_msg(JSONObject msg);

  protected abstract JSONObject _receive_msg();

  protected abstract DiscoveredServer discover_server();

  protected static class DiscoveredServer{
    private InetAddress serverAddress;
    private InetAddress ownAddress;
    private String macAddress;

    public DiscoveredServer(InetAddress serverAddress, InetAddress ownAddress, String macAddress) {
      this.serverAddress = serverAddress;
      this.ownAddress = ownAddress;
      this.macAddress = macAddress;
    }

    public InetAddress getServerAddress() {
      return serverAddress;
    }

    public InetAddress getOwnAddress() {
      return ownAddress;
    }

    public String getMacAddress() {
      return macAddress;
    }
  }

  protected InetAddress getServer_address() {
    return server_address;
  }

  void send_keepalive(String device_name) {
    log.debug(String.format("Sending keep_alive for |%s|", device_name));
    try {
      JSONObject keep_alive = new JSONObject();
      keep_alive.put(Const.CONN_TYPE, Const.CONN_KEEP_ALIVE);
      keep_alive.put(Const.GLOBAL_ID, service.getGlobalId(device_name));
      _send_msg(keep_alive);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private int connect_device(AdvertiseDevice device, InetAddress ip, String hw_addr, int global_id) throws JSONException {
    // TODO client - do not send adapter config upon reconnect

    // send hello message
    JSONObject hello_msg = new JSONObject();
    if(global_id>0){
      hello_msg.put(Const.GLOBAL_ID, global_id);
    }
    hello_msg.put(Const.DEV_IP, ip.toString());
    hello_msg.put(Const.DEV_HW_ADDRESS, hw_addr.toLowerCase());
    hello_msg.put(Const.DEV_TYPE, device.getType());
    hello_msg.put(Const.LOCAL_ID, device.getLocalId());
    if(device.getHost()!=null) {
      hello_msg.put(Const.HOST, device.getHost().getGlobalId());
    }
    hello_msg.put(Const.CONN_TYPE, Const.CONN_HELLO);

    this._send_msg(hello_msg);

    JSONObject hello_reply = this._receive_msg();
    if (hello_reply != null) {
      if (hello_reply.has(Const.GLOBAL_ID)) { //check for valid server response
        global_id = hello_reply.getInt(Const.GLOBAL_ID);
      }
      if (hello_reply.has(Const.ERROR) && global_id <= 0) {
        log.info("Could not connect device |{}|. Reason |{}|", device.getLocalId(), hello_reply.getString(Const.ERROR));
      }
    }else{
      log.error("No response on hello_message. No connection possible");
      global_id = 0;
    }

    if (global_id > 0) {
      // send init message
      JSONObject init_msg = new JSONObject();
      init_msg.put(Const.GLOBAL_ID, global_id);
      init_msg.put(Const.CONN_TYPE, Const.CONN_INIT);

      Map<String, Object> adapter_conf = device.getAdapterConf();
      for(String key: adapter_conf.keySet()){
        init_msg.put(key, adapter_conf.get(key));
      }

      this._send_msg(init_msg);
      log.debug("Waiting for ACK");
      JSONObject ack = this._receive_msg();
      if (ack == null || !ack.has(Const.GLOBAL_ID) || ack.getInt(Const.GLOBAL_ID) != global_id) {
        log.debug("Did not recieve valid ACK. Treating device as unconnected");
        global_id = 0;  // treat as unconnected
      }
    }
    return global_id;
  }

  void advertise() throws JSONException {
    int tries = 0;
    while (this.server_address == null && tries < 5) {
      tries++;
      log.debug("discovering server; try |{}|", tries);
      DiscoveredServer server = this.discover_server();
      if (server != null) {
        this.server_address = server.getServerAddress();
        this.ip = server.getOwnAddress();
        this.hw_addr = server.getMacAddress();
      }
      try {
        Thread.sleep(Const.SLEEPTIME * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    log.info("Server found @ |{}| after |{}| tries", this.server_address, tries);

    if (this.server_address != null) {
      AdvertiseDevice host = this.service.getHost();
      if (host != null) {
        int global_id = host.getGlobalId();
        if (global_id > 0) {
          log.info("Reconnecting device |{}|", host.getLocalId());
        } else {
          log.info("Connecting device |{}|", host.getLocalId());
        }

        global_id = this.connect_device(host, this.ip, this.hw_addr, global_id);

        this.service.setConnected(host.getLocalId(), global_id>0);
        this.service.setGlobalId(host.getLocalId(), global_id);
        if (global_id > 0) {
          log.info("Connected device |{}| with GLOBAL_ID |{}|", host.getLocalId(), global_id);
        } else {
          log.error("Could not connect host. Aborting advertising...");
          return;
        }
      }

      Map<String, AdvertiseDevice> devices = this.service.getDevices();
      for (String localId: devices.keySet()) {
        AdvertiseDevice device = devices.get(localId);

        if (host != null) {
          // add host to the device
          device.setHost(host);
        } else {
          device.setHost(null);
        }
        int global_id = this.service.getGlobalId(device.getLocalId());
        if (global_id > 0) {
          log.info("Reconnecting device |{}|", device.getLocalId());
        } else {
          log.info("Connecting device |{}|", device.getLocalId());
        }
        global_id = this.connect_device(device, this.ip, this.hw_addr, global_id);
        this.service.setConnected(device.getLocalId(), global_id>0);
        this.service.setGlobalId(device.getLocalId(), global_id);
        if (global_id > 0) {
          log.info("Connected device |{}| with GLOBAL_ID |{}|", device.getLocalId(), global_id);
        } else{
          log.warn("Failed to connect device |{}|");
        }
      }
    }
  }
}
