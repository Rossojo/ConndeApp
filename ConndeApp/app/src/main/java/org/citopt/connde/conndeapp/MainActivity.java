package org.citopt.connde.conndeapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.citopt.connde.conndeapp.advertise.AdvertiseService;
import org.citopt.connde.conndeapp.advertise.Const;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "ConndeMain";
  private SensorManager mSensorManager;

  private String curSsid;
  private AdvertiseService advertiseService;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Log.i(TAG, "Registering Broadcast receiver");
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    registerReceiver(new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        wifiConnectionChanged(context, intent);
      }
    }, intentFilter);

    try {
      ensureAutodeployConf();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if(advertiseService!=null){
      try {
        advertiseService.stop();
      } catch (JSONException e) {
        Log.e(TAG, "Error stopping advertise service");
      }
    }
  }

  private void ensureAutodeployConf() throws JSONException {
    File filesDir = getApplicationContext().getFilesDir();

    File autodeployFile = new File(filesDir, Const.AUTODEPLOY_FILE);
    if (!autodeployFile.exists()){
      Log.i(TAG, "Generating autodeploy file");
      mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

      JSONObject autodeployConf = new JSONObject();

      JSONObject host = new JSONObject();
      host.put(Const.LOCAL_ID, Build.DEVICE);
      host.put(Const.TYPE, Build.MODEL);

      JSONObject hostAdapterConf = new JSONObject();
      hostAdapterConf.put(Const.TIMEOUT, 30); // default 30 seconds
      host.put(Const.ADAPTER_CONF, hostAdapterConf);

      autodeployConf.put(Const.DEPLOY_SELF, host);

      JSONArray deployDevices = new JSONArray();
      for (Sensor sensor : sensors) {
        JSONObject jsonSensor= new JSONObject();
        jsonSensor.put(Const.LOCAL_ID, sensor.getName());
        jsonSensor.put(Const.TYPE, getStringType(sensor.getType()));

        JSONObject adapterConf = new JSONObject();
        adapterConf.put(Const.TIMEOUT, 30); // default 30 seconds
        jsonSensor.put(Const.ADAPTER_CONF, adapterConf);
        deployDevices.put(jsonSensor);
      }

      autodeployConf.put(Const.DEPLOY_DEVICES, deployDevices);

      String jsonString = autodeployConf.toString(2);
      try(OutputStream os= new FileOutputStream(autodeployFile)){
        os.write(jsonString.getBytes(Charset.forName("UTF-8")));
        os.flush();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      Log.i(TAG, "Successfully generated autodeploy file |\n" + autodeployConf.toString(4) + "\n|");
    }else {
      Log.w(TAG, "Autodeploy file exists");
    }
  }

  private String getStringType(int sensorType){
    switch(sensorType){
      case Sensor.TYPE_ACCELEROMETER:
        return Sensor.STRING_TYPE_ACCELEROMETER;
      case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          return Sensor.STRING_TYPE_ACCELEROMETER_UNCALIBRATED;
        }else{
          return "android.sensor.accelerometer_uncalibrated";
        }
      case Sensor.TYPE_AMBIENT_TEMPERATURE:
        return Sensor.STRING_TYPE_AMBIENT_TEMPERATURE;
      case Sensor.TYPE_DEVICE_PRIVATE_BASE:
        return "Unknown Sensor type |Device_Private_Base|";
      case Sensor.TYPE_GAME_ROTATION_VECTOR:
        return Sensor.STRING_TYPE_GAME_ROTATION_VECTOR;
      case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
        return Sensor.STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR;
      case Sensor.TYPE_GRAVITY:
        return Sensor.STRING_TYPE_GRAVITY;
      case Sensor.TYPE_GYROSCOPE:
        return Sensor.STRING_TYPE_GYROSCOPE;
      case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
        return Sensor.STRING_TYPE_GYROSCOPE_UNCALIBRATED;
      case Sensor.TYPE_HEART_BEAT:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          return Sensor.STRING_TYPE_HEART_BEAT;
        }else{
          return "android.sensor.heart_beat";
        }
      case Sensor.TYPE_HEART_RATE:
        return Sensor.STRING_TYPE_HEART_RATE;
      case Sensor.TYPE_LIGHT:
        return Sensor.STRING_TYPE_LIGHT;
      case Sensor.TYPE_LINEAR_ACCELERATION:
        return Sensor.STRING_TYPE_LINEAR_ACCELERATION;
      case Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT:
        return Sensor.STRING_TYPE_LOW_LATENCY_OFFBODY_DETECT;
      case Sensor.TYPE_MAGNETIC_FIELD:
        return Sensor.STRING_TYPE_MAGNETIC_FIELD;
      case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
        return Sensor.STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED;
      case Sensor.TYPE_MOTION_DETECT:
        return Sensor.STRING_TYPE_MOTION_DETECT;
      case Sensor.TYPE_ORIENTATION:
        return Sensor.STRING_TYPE_ORIENTATION;
      case Sensor.TYPE_POSE_6DOF:
        return Sensor.STRING_TYPE_POSE_6DOF;
      case Sensor.TYPE_PRESSURE:
        return Sensor.STRING_TYPE_PRESSURE;
      case Sensor.TYPE_PROXIMITY:
        return Sensor.STRING_TYPE_PROXIMITY;
      case Sensor.TYPE_RELATIVE_HUMIDITY:
        return Sensor.STRING_TYPE_RELATIVE_HUMIDITY;
      case Sensor.TYPE_ROTATION_VECTOR:
        return Sensor.STRING_TYPE_ROTATION_VECTOR;
      case Sensor.TYPE_SIGNIFICANT_MOTION:
        return Sensor.STRING_TYPE_SIGNIFICANT_MOTION;
      case Sensor.TYPE_STATIONARY_DETECT:
        return Sensor.STRING_TYPE_STATIONARY_DETECT;
      case Sensor.TYPE_STEP_COUNTER:
        return Sensor.STRING_TYPE_STEP_COUNTER;
      case Sensor.TYPE_STEP_DETECTOR:
        return Sensor.STRING_TYPE_STEP_DETECTOR;
      case Sensor.TYPE_TEMPERATURE:
        return Sensor.STRING_TYPE_TEMPERATURE;
      default:
        Log.w(TAG, "Could not translate Sensor Type |" + sensorType + "|");
        return "Unknown Sensor Type |" + sensorType + "|";
    }
  }

  public void wifiConnectionChanged(Context context, Intent intent) {
    final String action = intent.getAction();
    if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
      NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
      if (netInfo.getState() == NetworkInfo.State.CONNECTED) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ssid = wifiManager.getConnectionInfo().getSSID();
        if(ssid != null && !ssid.equals(curSsid)) {
          Log.i(TAG, "Connected to |" + ssid + "|");
          curSsid = ssid;
          try {
            advertiseService = new AdvertiseService(getFilesDir());
          } catch (JSONException e) {
            Log.d(TAG, "Error constructing advertising service", e);
          }
          new Thread(new Runnable() {
            @Override
            public void run() {
              try {
                advertiseService.start("android");
              } catch (JSONException e) {
                Log.e(TAG, "Error starting advertising service", e);
              }
            }
          }).start();
        }
      } else {
        Log.d(TAG, "No Connection");
        curSsid = null;
          if(advertiseService != null) {
            try {
              advertiseService.stop();
            } catch (JSONException e) {
              Log.e(TAG, "Error stopping advertising service", e);
            }
          }
      }
    }
  }

  public void listSensors(View view) {
    TextView lblSensorlist = (TextView) findViewById(R.id.lblSensorlist);
    lblSensorlist.setText(getString(R.string.msg_model, Build.MODEL));

    String appendText = "\n\n";

    File filesDir = getApplicationContext().getFilesDir();
    File autodeployFile = new File(filesDir, Const.AUTODEPLOY_FILE);
    JSONObject readObject = new JSONObject();
    if (autodeployFile.exists()) {
      try (InputStream is = new FileInputStream(autodeployFile)) {
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        String json = new String(buffer, "UTF-8");
        try {
          readObject = new JSONObject(json);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if(readObject != null) {
      try {
        appendText += readObject.toString(4);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    lblSensorlist.append(appendText);

//    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//    List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//    String strSensors = "\n";
//    for (Sensor sensor : sensors) {
//      strSensors += sensor.getType() + "\n";
//    }
//
//    lblSensorlist.append("n" + strSensors);
//        String sb = "Your manufacturer is:\t\t" + Build.MANUFACTURER + "\n" +
//                "Your model is: \t\t" + Build.MODEL + "\n" +
//                "Your device is: \t\t" + Build.DEVICE + "\n" +
//                "Your brand is: \t\t" + Build.BRAND + "\n" +
//                "Your fingerprint is: \t\t" + Build.FINGERPRINT + "\n" +
//                "Your id is: \t\t" + Build.ID + "\n" +
//                "Your type is: \t\t" + Build.TYPE + "\n" +
//                "Your product is: \t\t" + Build.PRODUCT + "\n" +
//                "Your host is: \t\t" + Build.HOST + "\n" +
//                "Your user is: \t\t" + Build.USER + "\n";
//        lblSensorlist.setText(sb);
  }

  private InetAddress getBroadcastAddress() throws IOException {
    WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    DhcpInfo dhcp = wifi.getDhcpInfo();
    if(dhcp != null) {
      // handle null somehow

      int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
      byte[] quads = new byte[4];
      for (int k = 0; k < 4; k++)
        quads[k] = (byte) (broadcast >> (k * 8));
      return InetAddress.getByAddress(quads);
    }
    return null;
  }
}
