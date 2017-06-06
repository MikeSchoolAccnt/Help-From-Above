package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

public class WifiP2pConnectActivity extends AppCompatActivity {
    private static final String TAG = "WifiP2pConnectActivity";
    private WifiP2pConnectActivityBroadcastReceiver broadcastReceiver = new WifiP2pConnectActivityBroadcastReceiver();
    private IntentFilter intentFilter = new IntentFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_p2p_connect);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    private void handleWifiP2pStateChanged() {
        Log.d(TAG, "handleWifiP2pStateChanged");
    }

    private void handleWifiP2pPeersChanged() {
        Log.d(TAG, "handleWifiP2pPeersChanged");
    }

    private void handleWifiP2pConnectionChanged() {
        Log.d(TAG, "handleWifiP2pConnectionChanged");
    }

    private void handleWifiP2pThisDeviceChanged() {
        Log.d(TAG, "handleWifiP2pThisDeviceChanged");
    }

    private class WifiP2pConnectActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                        handleWifiP2pStateChanged();
                        break;
                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        handleWifiP2pPeersChanged();
                        break;
                    case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                        handleWifiP2pConnectionChanged();
                        break;
                    case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                        handleWifiP2pThisDeviceChanged();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;

                }
            }
        }
    }
}
