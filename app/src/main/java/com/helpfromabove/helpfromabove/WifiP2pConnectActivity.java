package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.Collection;

public class WifiP2pConnectActivity extends AppCompatActivity {
    private static final String TAG = "WifiP2pConnectActivity";
    private WifiP2pConnectActivityBroadcastReceiver broadcastReceiver = new WifiP2pConnectActivityBroadcastReceiver();
    private IntentFilter intentFilter = new IntentFilter();
    private ListView devicesListView;
    private ArrayAdapter adapter;
    private Collection<WifiP2pDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_p2p_connect);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        adapter = new ArrayAdapter(getApplicationContext(), R.layout.wifi_p2p_device, R.id.wifi_p2p_device_name) {
            @Override
            public void notifyDataSetChanged() {
                Log.d(TAG, "notifyDataSetChanged");
                super.notifyDataSetChanged();

                onArrayAdapterDataSetChanged();
            }
        };
        try {
            devicesListView = (ListView) findViewById(R.id.wifi_p2p_connect_listview);
            devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "onItemClick");
                    wifiP2pDeviceOnClick(view);
                }
            });
            devicesListView.setAdapter(adapter);

            // This is for ListView items testing. Remove once testing is complete
            adapter.add("test123");
            adapter.add("test456");
            adapter.add("test789");
        } catch (NullPointerException nPE) {
            Log.e(TAG, "onCreate: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        registerReceiver(broadcastReceiver, intentFilter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    public void wifiP2pDeviceOnClick(View view) {
        Log.d(TAG, "wifiP2pDeviceOnClick: view=" + view);
    }

    private void onArrayAdapterDataSetChanged() {
        Log.d(TAG, "onArrayAdapterDataSetChanged");

        updateProgressBar();
    }

    private void updateProgressBar() {
        Log.d(TAG, "updateProgressBar");

        try {
            View loadingProgressBar = findViewById(R.id.wifi_p2p_connect_progressbar);
            ListAdapter adapter = devicesListView.getAdapter();
            if (adapter.isEmpty()) {
                Log.d(TAG, "updateProgressBar: Setting loadingProgressBar visibility to VISIBLE");
                loadingProgressBar.setVisibility(View.VISIBLE);
            } else {
                Log.d(TAG, "updateProgressBar: Setting loadingProgressBar visibility to INVISIBLE");
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        } catch (NullPointerException nPE) {
            Log.e(TAG, "updateProgressBar: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    private void handleWifiP2pDiscoveryChanged() {
        Log.d(TAG, "handleWifiP2pDiscoveryChanged");
    }

    private void handleWifiP2pStateChanged() {
        Log.d(TAG, "handleWifiP2pStateChanged");
    }

    private void handleWifiP2pPeersChanged(Collection<WifiP2pDevice> devices) {
        this.devices = devices;
        for (WifiP2pDevice device : devices) {
            Log.d(TAG, "handleWifiP2pPeersChanged: device.toString()=" + device.toString());

            // TODO : Test to make sure that this displays properly
            adapter.add(device.toString());
        }
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
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                        handleWifiP2pDiscoveryChanged();
                        break;
                    case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                        handleWifiP2pStateChanged();
                        break;
                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        WifiP2pDeviceList deviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                        handleWifiP2pPeersChanged(deviceList.getDeviceList());
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
