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
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
                super.notifyDataSetChanged();

                onArrayAdapterDataSetChanged();
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                WifiP2pDevice device = this.getItem(position);
                if (device != null) {
                    TextView name = (TextView) view.findViewById(R.id.wifi_p2p_device_name);
                    name.setText(device.deviceName);
                    TextView address = (TextView) view.findViewById(R.id.wifi_p2p_device_address);
                    address.setText(device.deviceAddress);
                }
                return view;
            }
        };

        try {
            devicesListView = (ListView) findViewById(R.id.wifi_p2p_connect_listview);
            devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    wifiP2pDeviceOnClick(position);
                }
            });
            devicesListView.setAdapter(adapter);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "onCreate: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindCommandService();
    }

    @Override
    protected void onResume() {
        super.onResume();

        broadcastReceiver = new WifiP2pConnectActivityBroadcastReceiver();
        registerReceiver(broadcastReceiver, intentFilter);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindCommandService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissWifiP2pConnectingDialog();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void bindCommandService() {
        Intent commandServiceIntent = new Intent(getApplicationContext(), CommandService.class);
        startService(commandServiceIntent);
        commandServiceConnection = new WifiP2pConnectActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void unbindCommandService() {
        unbindService(commandServiceConnection);
    }

    private void setConnectedService(IBinder service) {
        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
            commandService.startWifiP2pScanning();
        }
    }

    public void wifiP2pDeviceOnClick(int position) {
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

    private void createWifiP2pConnectingDialog() {
        if (wifiP2pConnectingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(new ProgressBar(getApplicationContext()))
                    .setCancelable(false);
            wifiP2pConnectingDialog = builder.create();
        }
    }

    private void setWifiP2pConnectingDialogTitle(int id) {
        if (wifiP2pConnectingDialog == null) {
            createWifiP2pConnectingDialog();
        }

        wifiP2pConnectingDialog.setTitle(id);
    }

    private void showWifiP2pConnectingDialogDialog() {
        if (wifiP2pConnectingDialog == null) {
            createWifiP2pConnectingDialog();
        }

        if (!wifiP2pConnectingDialog.isShowing()) {
            wifiP2pConnectingDialog.show();
        }

        hideDeviceListView();
        hideDeviceListViewLoadingProgressBar();
    }


    private void dismissWifiP2pConnectingDialog() {
        if (wifiP2pConnectingDialog == null) {
            createWifiP2pConnectingDialog();
        }

        if (wifiP2pConnectingDialog.isShowing()) {
            wifiP2pConnectingDialog.dismiss();
        }

        showDeviceListView();
    }

    private void onArrayAdapterDataSetChanged() {
        updateProgressBar();
    }

    private void updateProgressBar() {
        try {
            ListAdapter adapter = devicesListView.getAdapter();
            if (adapter.isEmpty()) {
                hideDeviceListView();
                showDeviceListViewLoadingProgressBar();
            } else {
                hideDeviceListViewLoadingProgressBar();
                showDeviceListView();
            }
        } catch (NullPointerException nPE) {
            Log.e(TAG, "updateProgressBar: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    private void showDeviceListView() {
        try {
            View connectListView = findViewById(R.id.wifi_p2p_connect_listview);
            connectListView.setVisibility(View.VISIBLE);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "showDeviceListView: NullPointerException: " + nPE.getMessage(), nPE);
        }

    }

    private void hideDeviceListView() {
        try {
            View connectListView = findViewById(R.id.wifi_p2p_connect_listview);
            connectListView.setVisibility(View.INVISIBLE);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "hideDeviceListView: NullPointerException: " + nPE.getMessage(), nPE);
        }

    }

    private void showDeviceListViewLoadingProgressBar() {
        try {
            View loadingProgressBar = findViewById(R.id.wifi_p2p_connect_progressbar);
            loadingProgressBar.setVisibility(View.VISIBLE);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "showDeviceListViewLoadingProgressBar: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    private void hideDeviceListViewLoadingProgressBar() {
        try {
            View loadingProgressBar = findViewById(R.id.wifi_p2p_connect_progressbar);
            loadingProgressBar.setVisibility(View.INVISIBLE);
        } catch (NullPointerException nPE) {
            Log.e(TAG, "hideDeviceListViewLoadingProgressBar: NullPointerException: " + nPE.getMessage(), nPE);
        }
    }

    private void handleWifiP2pPeersChanged(Collection<WifiP2pDevice> devices) {
        adapter.clear();
        for (WifiP2pDevice device : devices) {
            adapter.add(device);
        }
    }

    private void handleWifiP2pStateChanged() {
        if (commandService != null) {
            switch (commandService.getState().getWifiP2pState()) {
                case WIFI_P2P_CONNECTING_TO_UASC:
                    setWifiP2pConnectingDialogTitle(R.string.wifi_p2p_connecting_to_uasc_dialog_title);
                    showWifiP2pConnectingDialogDialog();
                    break;
                case WIFI_P2P_WAITING_FOR_UASC:
                    setWifiP2pConnectingDialogTitle(R.string.wifi_p2p_waiting_on_uasc_dialog_title);
                    showWifiP2pConnectingDialogDialog();
                    break;
                case WIFI_P2P_CONNECTING_FROM_UASC:
                    setWifiP2pConnectingDialogTitle(R.string.wifi_p2p_connecting_from_uasc_dialog_title);
                    showWifiP2pConnectingDialogDialog();
                    break;
                case WIFI_P2P_CONNECTED:
                    dismissWifiP2pConnectingDialog();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                    break;
                default:
                    Log.w(TAG, "handleWifiP2pStateChanged: default");
            }
        }
    }

    private class WifiP2pConnectActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            setConnectedService(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
