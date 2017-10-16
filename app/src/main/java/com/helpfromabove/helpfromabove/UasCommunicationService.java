package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class UasCommunicationService extends Service {
    private static final String TAG = "UasCommunicationService";

    private final IBinder mBinder = new UasCommunicationServiceBinder();

    private IntentFilter intentFilter;
    private UasCommunicationServiceBroadcastReceiver broadcastReceiver;

    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private WifiP2pManager.ActionListener wifiP2pScanListener = new WifiP2pScanActionListener();
    private WifiP2pManager.ActionListener wifiP2pConnectionListener = new WifiP2pConnectActionListener();

    private WifiP2pInfo wifiP2pInfo;
    private NetworkInfo networkInfo;
    private WifiP2pGroup wifiP2pGroup;

    //debugging variable. Remove before final testing.
    private boolean canConnect = false;

    public UasCommunicationService() {
        super();

        Log.d(TAG, "UasCommunicationService");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        broadcastReceiver = new UasCommunicationServiceBroadcastReceiver();
        registerReceiver(broadcastReceiver, intentFilter);

        setWifiManager();
        turnOnWifi();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        unregisterReceiver(broadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void setWifiManager() {
        Log.d(TAG, "setWifiManager");

        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    }

    private void turnOnWifi() {
        Log.d(TAG, "turnOnWifi");

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    protected void startScanning() {
        Log.d(TAG, "startScanning");

        wifiP2pManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        wifiP2pManager.discoverPeers(wifiP2pChannel, wifiP2pScanListener);
    }

    protected void connectToDevice(WifiP2pDevice device) {
        Log.d(TAG, "connectToDevice: device.toString()=" + device.toString());

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(wifiP2pChannel, config, wifiP2pConnectionListener);
    }

    private void handleDiscoveryStateChanged(int state) {
        switch (state) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                Log.d(TAG, "handleDiscoveryStateChanged: WIFI_P2P_DISCOVERY_STARTED");
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                Log.d(TAG, "handleDiscoveryStateChanged: WIFI_P2P_DISCOVERY_STOPPED");
                break;
            default:
                Log.w(TAG, "handleDiscoveryStateChanged: default: state=" + state);
                break;
        }
    }

    private void handleWifiP2pStateChanged(int state) {
        switch (state) {
            case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                Log.d(TAG, "handleWifiP2pStateChanged: WIFI_P2P_STATE_ENABLED");
                break;
            case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                Log.d(TAG, "handleWifiP2pStateChanged: WIFI_P2P_STATE_DISABLED");
                break;
            default:
                Log.w(TAG, "handleWifiP2pStateChanged: default: state=" + state);
                break;
        }
    }

    private void handleWifiP2pConnectionChangedAction(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo, WifiP2pGroup wifiP2pGroup) {
        Log.d(TAG, "handleWifiP2pConnectionChangedAction");

        Log.d(TAG, "handleWifiP2pConnectionChangedAction: groupFormed=" + wifiP2pInfo.groupFormed);
        Log.d(TAG, "handleWifiP2pConnectionChangedAction: groupOwnerAddress=" + wifiP2pInfo.groupOwnerAddress);
        Log.d(TAG, "handleWifiP2pConnectionChangedAction: networkInfo=" + networkInfo);
        Log.d(TAG, "handleWifiP2pConnectionChangedAction: wifiP2pGroup=" + wifiP2pGroup);

        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            this.wifiP2pInfo = wifiP2pInfo;
            this.networkInfo = networkInfo;
            this.wifiP2pGroup = wifiP2pGroup;
            CommandService.notifyUiWifiP2pConnected(getApplicationContext());
        }
    }

    private void handleThisDeviceDetailsChanged() {
        Log.d(TAG, "handleThisDeviceDetailsChanged");
    }

    protected void startSession() {
        Log.d(TAG, "startSession: NOT IMPLEMENTED!");
    }

    protected void stopSession() {
        Log.d(TAG, "stopSession: NOT IMPLEMENTED!");
    }

    protected void setLightOnOff(boolean lightOnOff) {
        Log.d(TAG, "lightOnOff: lightOnOff=" + lightOnOff + ": NOT IMPLEMENTED!");
    }

    protected void sendWaypoint(Location location) {
        Log.d(TAG, "sendWaypoint: location=" + location + ": NOT IMPLEMENTED!");
    }

    protected void startEmergency() {
        Log.d(TAG, "startEmergency: NOT IMPLEMENTED!");
    }

    protected class UasCommunicationServiceBinder extends Binder {
        UasCommunicationService getService() {
            return UasCommunicationService.this;
        }
    }

    private class UasCommunicationServiceBroadcastReceiver extends BroadcastReceiver {
        private static final String TAG = "Uas...BroadcastReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                        int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
                        handleDiscoveryStateChanged(discoveryState);
                        break;
                    case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                        int wifiState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                        handleWifiP2pStateChanged(wifiState);
                        break;
                    case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                        WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                        WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                        handleWifiP2pConnectionChangedAction(wifiP2pInfo, networkInfo, wifiP2pGroup);
                        break;
                    case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                        handleThisDeviceDetailsChanged();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }

    private class WifiP2pScanActionListener implements WifiP2pManager.ActionListener {
        private static final String TAG = "WifiP2ScanPActionLis...";

        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess");
        }

        @Override
        public void onFailure(int reasonCode) {
            Log.d(TAG, "onFailure: reasonCode=" + reasonCode);
            switch (reasonCode) {
                case WifiP2pManager.P2P_UNSUPPORTED:
                    Log.w(TAG, "onFailure: P2P_UNSUPPORTED");
                    break;
                case WifiP2pManager.BUSY:
                    Log.w(TAG, "onFailure: BUSY");
                    break;
                case WifiP2pManager.ERROR:
                    Log.w(TAG, "onFailure: ERROR");
                    break;
                default:
                    Log.w(TAG, "onFailure: default");
            }
        }
    }

    private class WifiP2pConnectActionListener implements WifiP2pManager.ActionListener {
        private static final String TAG = "WifiP2ConnectPAction...";

        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess");
        }

        @Override
        public void onFailure(int reasonCode) {
            Log.d(TAG, "onFailure: reasonCode=" + reasonCode);
            switch (reasonCode) {
                case WifiP2pManager.P2P_UNSUPPORTED:
                    Log.w(TAG, "onFailure: P2P_UNSUPPORTED");
                    break;
                case WifiP2pManager.BUSY:
                    Log.w(TAG, "onFailure: BUSY");
                    break;
                case WifiP2pManager.ERROR:
                    Log.w(TAG, "onFailure: ERROR");
                    break;
                default:
                    Log.w(TAG, "onFailure: default");
            }

            // TODO: Used for testing other features on emulator, remove in final production
            CommandService.notifyUiWifiP2pConnected(getApplicationContext());
        }
    }
}
