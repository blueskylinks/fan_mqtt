package com.blueskylinks.fan_mqtt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    byte sc1[];
    int j;
    MqttClient sampleClient;
    TextView tv;
    MqttMessage Mmessage1;
    Switch simpleswitch1;
    Switch simpleswitch2;
    int lr[]=new int[6];
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner scanner;
    private BluetoothDevice ble_device;
    ScanRecord scan_rec;
    SharedPreferences sharedPreferences1;
    SharedPreferences sharedPreferences2;
    boolean value1 = true;
    boolean value2 = true;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},200);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},200);
        initialize();
        mqtt_connect();

        simpleswitch1=findViewById(R.id.switch1);
        simpleswitch2=findViewById(R.id.switch2);
        sharedPreferences1 = getSharedPreferences("isChecked1", 0);
        value1 = sharedPreferences1.getBoolean("isChecked1", value1); // retrieve the value of your key

        sharedPreferences2 = getSharedPreferences("isChecked", 0);
        value2 = sharedPreferences2.getBoolean("isChecked", value2); // retrieve the value of your key

        simpleswitch1.setChecked(value1);
        simpleswitch1.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) { isChecked=true; startscand();simpleswitch2.setClickable(true);sharedPreferences1.edit().putBoolean("isChecked1", true).apply(); }
                        else { stopscand();simpleswitch2.setClickable(false);sharedPreferences1.edit().putBoolean("isChecked1", false).apply();}
                    }
                }
        );

        if( simpleswitch1.isChecked()){
            startscand();
        }
       simpleswitch2.setChecked(value2);
            simpleswitch2.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {isChecked = true;
                                sharedPreferences2.edit().putBoolean("isChecked", true).apply();
                            } else sharedPreferences2.edit().putBoolean("isChecked", false).apply();
                        }
                    }
            );
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume(){
        super.onResume();

    }

      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        Log.i("BleScanning:", "initilizing.......");
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.i("BleScanning:", "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.i("BleScanning:", "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            mBluetoothAdapter.enable();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    //================  Start BLE Scanning  ===============
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startscand() {
        Log.i("BLE------", "Start Scanning");
        final ParcelUuid UID_SERVICE =
        ParcelUuid.fromString("000000f1-0000-1000-8000-00805f9b34fb");
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        ScanFilter beaconFilter = new ScanFilter.Builder() // this filter will be used to get only specific device based on service UUID
                .setServiceUuid(UID_SERVICE)
                .build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(beaconFilter);
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        scanner.startScan(filters, settings, mcallback);
    }



    // =============== BLE Callback =======================
    // This callback method will be automatically called every time the scanner get the device adv data
    public ScanCallback mcallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            int rssi;
            rssi = result.getRssi();
            scan_rec = result.getScanRecord();
            Log.i("Scan result",String.valueOf(rssi));
            Log.i("record",scan_rec.toString());
            tv=findViewById(R.id.tv);
            tv.setText(String.valueOf(rssi));
            sc1=scan_rec.getManufacturerSpecificData(0);
            for (int i=0;i<sc1.length; i++){
                lr[i]=sc1[i];
            }
            ble_device = result.getDevice();
            if(simpleswitch2.isChecked()) {
                try {
                    if (j != lr[0]) {
                        if(sampleClient.isConnected()){
                            Log.i("Blue Test:", "MQTT is connected");
                            msg_pub();
                        }
                        else{
                            Log.i("Blue Test:", "Not connected ...............");
                            mqtt_connect();
                            msg_pub();
                        }
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
            else return;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopscand() {
        Log.i("BLE-----", "Stop Scanning");
        scanner.stopScan(mcallback);
    }

    //publishing msgs
    public void mqtt_connect(){
        String broker       = "tcp://13.126.9.228:1883";
        String clientId     = "4";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            Log.i("Connecting to broker: ", broker);
            sampleClient.connect(connOpts);
            Log.i("Connected", "C");
            //sampleClient.subscribe("test");


        } catch(MqttException me) {
            Log.i("reason ",String.valueOf(me.getReasonCode()));
            Log.i("msg ",String.valueOf(me.getMessage()));
            Log.i("loc ",String.valueOf(me.getLocalizedMessage()));
            Log.i("cause ",String.valueOf(me.getCause()));
            Log.i("excep ",String.valueOf(me));
            me.printStackTrace();
        }

    }

    public void msg_pub()throws MqttException {
        String topic = "home";
        Mmessage1 = new MqttMessage();
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("lr[0]",lr[0]);
            jsonObject.put("lr[1]",lr[1]);
            jsonObject.put("lr[2]",lr[2]);
            String json=String.valueOf(jsonObject);
            Mmessage1.setPayload(json.getBytes());
            sampleClient.publish(topic, Mmessage1);
            Log.i("message sending ",String.valueOf(Mmessage1));
             j =jsonObject.getInt("lr[0]");
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (MqttException e) {
            e.printStackTrace();
        }
    }
}



