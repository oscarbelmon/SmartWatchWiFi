package es.uji.al259348.androidwearwifitest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;

public class MainActivity extends WearableActivity implements MqttCallback, IMqttActionListener, WifiScanReceiver.WifiScanListener {

    private static final String MQTT_URI = "tcp://192.168.0.114:61613";
    private static final String MQTT_USER = "admin";
    private static final String MQTT_PASS = "password";
    private static final int NUM_SCANS = 10;

    private WifiScanReceiver wifiScanReceiver;
    private int currentScan = 0;
    private long scanStartTime = 0;

    private MqttConnectOptions connectOptions;
    private MqttAndroidClient client;

    private Button btnConnect;
    private Button btnSend;
    private Button btnCheckConnection;
    private Button btnWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        btnCheckConnection = (Button) findViewById(R.id.btnCheckConnection);
        btnCheckConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkConnection();
            }
        });

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setEnabled(false);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsg("Hola desde Android!!");
            }
        });

        btnWifi = (Button) findViewById(R.id.btnWifi);
        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanWifi();
            }
        });

        // Wifi Scan

        wifiScanReceiver = new WifiScanReceiver(this);

        // MQTT client

        connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        connectOptions.setUserName(MQTT_USER);
        connectOptions.setPassword(MQTT_PASS.toCharArray());

        client = new MqttAndroidClient(this, MQTT_URI, "publisher",  new MemoryPersistence());
        client.setCallback(this);

    }

    private void connect() {
        Log.i("ASDF", "Conectando...");
        try {
            client.connect(connectOptions, this, this);

            btnConnect.setEnabled(false);
            btnSend.setEnabled(true);

        } catch (MqttException e) {
            e.printStackTrace();

        }
    }

    private void checkConnection() {
        if (client.isConnected()) {
            Toast.makeText(MainActivity.this, "Estás conectado.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Estás desconectado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMsg(String msg) {
        try {
            client.publish("iot", msg.getBytes(), 2, true);
            Log.i("ASDF", "Mensaje enviado..");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Log.i("ASDF", "Desconectado!");
        Toast.makeText(MainActivity.this, "Desconectado del servidor.", Toast.LENGTH_SHORT).show();
        btnConnect.setEnabled(true);
        btnSend.setEnabled(false);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Toast.makeText(MainActivity.this, "Message arrived..", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Toast.makeText(MainActivity.this, "Delivery complete..", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSuccess(IMqttToken iMqttToken) {
        Log.i("ASDF", "Conectado!");
        Toast.makeText(MainActivity.this, "Conectado!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
        MqttException e = (MqttException) throwable;
        Log.i("ASDF", "Fallo al conectar: " + e.getMessage());
        Log.i("ASDF", "Fallo al conectar: " + e.getLocalizedMessage());
        Log.i("ASDF", "Fallo al conectar: " + e.toString());
        Log.i("ASDF", "Fallo al conectar: " + e.getReasonCode());
        Log.i("ASDF", "Fallo al conectar: " + e.getCause());
        Toast.makeText(MainActivity.this, "Fallo al conectar: " + throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    public void scanWifi() {
        if (currentScan == 0) {
            registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            Date startDate = new Date();
            scanStartTime = startDate.getTime();
            Log.i("LecturaWiFiTiempo", startDate.toString() + " | Empezamos la lectura de WiFi. Num de muestras: " + NUM_SCANS);
        }

        WifiManager wifiManager = (WifiManager) getSystemService(Activity.WIFI_SERVICE);
        wifiManager.startScan();
        currentScan++;
        Log.i("LecturaWiFiTiempo", new Date().toString() + " | Empieza la lectura " + currentScan);
    }

    @Override
    public void onWifiScanFinish() {

        Log.i("LecturaWiFiTiempo", new Date().toString() + " | Termnina la lectura " + currentScan);
        WifiManager wifiManager = (WifiManager) getSystemService(Activity.WIFI_SERVICE);
        for (ScanResult res : wifiManager.getScanResults()) {
            Log.i("LecturaWiFiDatos", "Lectura " + currentScan + ": " + res.toString());
            if (client.isConnected())
                sendMsg(res.toString());
        }

        if (currentScan == NUM_SCANS) {
            unregisterReceiver(wifiScanReceiver);
            currentScan = 0;
            Date finishDate = new Date();
            long diff = finishDate.getTime() - scanStartTime;
            Log.i("LecturaWiFiTiempo", finishDate.toString() + " | Fin de lectura WiFi. Tiempo total en tomar " + NUM_SCANS + " muestras: " + diff + "ms");
        } else {
            scanWifi();
        }

    }
}
