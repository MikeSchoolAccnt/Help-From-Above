package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.Collection;

public class WifiP2pConnectActivity extends AppCompatActivity {
    private static final String TAG = "WifiP2pConnectActivity";

    CommandService commandService;
    ServiceConnection commandServiceConnection;

    private WifiP2pConnectActivityBroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
    private ListView devicesListView;
    private ArrayAdapter<WifiP2pDevice> adapter;
    private AlertDialog wifiP2pConnectingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_p2p_connect);

        setupActionBar();

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(CommandService.ACTION_WIFI_P2P_STATE_CHANGED);

        adapter = new ArrayAdapter<WifiP2pDevice>(getApplicationContext(), R.layout.wifi_p2p_device, R.id.wifi_p2p_device_name) {
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
                    wifiP2pDeviceOnClick(position);
                }
            });
            devicesListView.setAdapter(adapter);

            // This is for ListView items testing. Remove once testing is complete
            WifiP2pDevice test123 = new WifiP2pDevice();
            test123.deviceName = "test123";
            test123.deviceAddress = "123";
            WifiP2pDevice test456 = new WifiP2pDevice();
            test456.deviceName = "test456";
            test456.deviceAddress = "456";
            WifiP2pDevice test789 = new WifiP2pDevice();
            test789.deviceName = "test789";
            test789.deviceAddress = "789";

            adapter.add(test123);
            adapter.add(test456);
            adapter.add(test789);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "onCreate: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        bindCommandService();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        broadcastReceiver = new WifiP2pConnectActivityBroadcastReceiver();
        registerReceiver(broadcastReceiver, intentFilter);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        unbindCommandService();
    }

    private void setupActionBar() {
        Log.d(TAG, "setupActionBar");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void bindCommandService() {
        Log.d(TAG, "bindCommandService");
        Intent commandServiceIntent = new Intent(getApplicationContext(), CommandService.class);
        startService(commandServiceIntent);
        commandServiceConnection = new WifiP2pConnectActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void unbindCommandService() {
        Log.d(TAG, "unbindCommandService");

        unbindService(commandServiceConnection);
    }

    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
            commandService.startWifiP2pScanning();
        }
    }

    public void wifiP2pDeviceOnClick(int position) {
        Log.d(TAG, "wifiP2pDeviceOnClick: position=" + position);

        try {
            WifiP2pDevice device = adapter.getItem(position);
            displayConfirmDialog(device);
        } catch (IndexOutOfBoundsException iOOBE) {
            Log.e(TAG, "wifiP2pDeviceOnClick: IndexOutOfBoundsException: " + iOOBE.getMessage(), iOOBE);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "wifiP2pDeviceOnClick: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    private void displayConfirmDialog(final WifiP2pDevice device) {
        Log.d(TAG, "displayConfirmDialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.wifi_p2p_connect_to_device_dialog_title, device.deviceName))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        commandService.connectToWifiP2pDevice(device);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void displayWifiP2pConnectingDialog() {
        Log.d(TAG, "displayWifiP2pConnectingDialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(new ProgressBar(getApplicationContext()))
                .setTitle(R.string.wifi_p2p_connecting_to_uasc_dialog_title)
                .setCancelable(false);
        wifiP2pConnectingDialog = builder.create();
        wifiP2pConnectingDialog.show();
    }

    private void setWifiP2pConnectingDialogTitle(int id) {
        if (wifiP2pConnectingDialog != null) {
            wifiP2pConnectingDialog.setTitle(id);
        }
    }

    private void hideWifiP2pConnectingDialog() {
        if (wifiP2pConnectingDialog != null) {
            wifiP2pConnectingDialog.dismiss();
        }
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

    private void handleWifiP2pPeersChanged(Collection<WifiP2pDevice> devices) {
        Log.d(TAG, "handleWifiP2pPeersChanged");

        adapter.clear();
        for (WifiP2pDevice device : devices) {
            Log.d(TAG, "handleWifiP2pPeersChanged: device.toString()=" + device.toString());
            adapter.add(device);
        }
    }

    private void handleWifiP2pStateChanged() {
        if (commandService != null) {
            switch (commandService.getState().getWifiP2pState()) {
                case WIFI_P2P_CONNECTING_TO_UASC:
                    displayWifiP2pConnectingDialog();
                    break;
                case WIFI_P2P_WAITING_FOR_UASC:
                    setWifiP2pConnectingDialogTitle(R.string.wifi_p2p_waiting_on_uasc_dialog_title);
                    break;
                case WIFI_P2P_CONNECTING_FROM_UASC:
                    setWifiP2pConnectingDialogTitle(R.string.wifi_p2p_connecting_from_uasc_dialog_title);
                    break;
                case WIFI_P2P_CONNECTED:
                    hideWifiP2pConnectingDialog();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    break;
                default:
                    Log.w(TAG, "handleWifiP2pStateChanged: default");
            }
        }
    }

    private class WifiP2pConnectActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        WifiP2pDeviceList deviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                        handleWifiP2pPeersChanged(deviceList.getDeviceList());
                        break;
                    case CommandService.ACTION_WIFI_P2P_STATE_CHANGED:
                        handleWifiP2pStateChanged();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }

    protected class WifiP2pConnectActivityServiceConnection implements ServiceConnection {
        private static final String TAG = "CommandServiceConnec...";

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            setConnectedService(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    }
}
