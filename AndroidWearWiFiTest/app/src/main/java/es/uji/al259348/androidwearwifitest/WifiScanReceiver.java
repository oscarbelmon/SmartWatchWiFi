package es.uji.al259348.androidwearwifitest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WifiScanReceiver extends BroadcastReceiver {

    public interface WifiScanListener {
        void onWifiScanFinish();
    }

    WifiScanListener listener;

    public WifiScanReceiver(WifiScanListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        listener.onWifiScanFinish();
    }

}