package org.citopt.connde.conndeapp.advertise;

/**
 * Created by rosso on 19.08.17.
 */

public interface Const {
  // message parameters
  String GLOBAL_ID = "global_id";
  String DEV_IP = "dev_ip";
  String DEV_HW_ADDRESS = "dev_hw_addr";
  String LOCAL_ID = "local_id";
  String TYPE = "type";
  String CONN_TYPE = "conn_type";
  String PING_MSG = "ping_msg";
  String HOST = "host";
  String ERROR = "error";

  // Connection types
  String CONN_HELLO = "hello";
  String CONN_INIT = "init";
  String CONN_VALUE = "value";
  String CONN_KEEP_ALIVE = "alive";
  String CONN_PING = "ping";

  // autodeploy parameter
  String DEPLOY_SELF = "self";
  String DEPLOY_ON = "on";
  String DEPLOY_DEVICES = "devices";
  String ADAPTER_CONF = "adapter_conf";
  String PINSET = "pinset";
  String DEV_TYPE = "type";
  String TIMEOUT = "timeout";

  // other stuff
  String ENCODING = "utf-8";
  String LAST_CONTACT = "last_contact";

  // bluetooth server constants
  String BT_UUID = "809a061f-d834-4ba6-8741-29f0f53ca1b9";
  String BT_SERVICE_NAME = "connde_discovery";
  String BT_SERVICE_DESCRIPTION = "Server used for device discovery in the connde application";

  // Comm types
  String LAN = "LAN";
  String BT = "BT";
  String TEST = "TEST";

  // client constants
  int SLEEPTIME = 2;
  int PORT = 20123;
  String AUTODEPLOY_FILE = "autodeploy.json";
  int CLIENT_TIMEOUT = 5;

  String CLIENT_CMD_ARG = "advertiser_class";

  String GLOBAL_ID_FILE = "global_ids.json";

}
