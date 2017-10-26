package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Caleb Smith on 5/13/2017.
 * <p>
 * The CommandService is the model of the application.
 * Its purpose is to broadcast intents to visible activities, store
 * objects that need to be persistent throughout the life of the
 * application, and perform calculations on locations for waypoints.
 */

public class CommandService extends Service {
    protected static final String ACTION_SERVICES_READY = "com.helpfromabove.helpfromabove.action.ACTION_SERVICES_READY";
    protected static final String ACTION_WIFI_P2P_CONNECTED = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_CONNECTED";
    protected static final String ACTION_NEW_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_IMAGE";
    protected static final String ACTION_LOCATION_CALIBRATION_COMPLETE = "com.helpfromabove.helpfromabove.action.ACTION_LOCATION_CALIBRATION_COMPLETE";
    protected static final String ACTION_NEW_WAYPOINT = "com.helpfromabove.helpfromabove.action.ACTION_NEW_WAYPOINT";
    protected static final String ACTION_NEW_UAS_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_LOCATION";
    protected static final String ACTION_NEW_HHMD_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_HHMD_LOCATION";

    private final static String TAG = "CommandService";

    private final IBinder mBinder = new CommandServiceBinder();
    ServiceConnection uasCommunicationServiceConnection;
    UasCommunicationService uasCommunicationService;
    ServiceConnection locationServiceConnection;
    LocationService locationService;
    ServiceConnection emergencyServiceConnection;
    EmergencyService emergencyService;
    ServiceConnection cloudServiceConnection;
    CloudService cloudService;


    private CommandServiceBroadcastReceiver commandServiceBroadcastReceiver;
    private IntentFilter intentFilter;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NEW_WAYPOINT);
        intentFilter.addAction(ACTION_NEW_UAS_LOCATION);
        intentFilter.addAction(ACTION_NEW_UAS_IMAGE);
        commandServiceBroadcastReceiver = new CommandServiceBroadcastReceiver();
        registerReceiver(commandServiceBroadcastReceiver, intentFilter);

        startServices();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        stopServices();
        unregisterReceiver(commandServiceBroadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void startServices() {
        startUasCommunicationService();
        startLocationService();
        startEmergencyService();
        startNetworkService();
    }

    private void startUasCommunicationService() {
        Log.d(TAG, "startUasCommunicationService");

        Intent uasCommunicationServiceIntent = new Intent(getApplicationContext(), UasCommunicationService.class);
        startService(uasCommunicationServiceIntent);
        uasCommunicationServiceConnection = new CommandServiceConnection();
        bindService(uasCommunicationServiceIntent, uasCommunicationServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void startLocationService() {
        Log.d(TAG, "startLocationService");

        Intent locationServiceIntent = new Intent(getApplicationContext(), LocationService.class);
        startService(locationServiceIntent);
        locationServiceConnection = new CommandServiceConnection();
        bindService(locationServiceIntent, locationServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void startEmergencyService() {
        Log.d(TAG, "startEmergencyService");

        Intent emergencyServiceIntent = new Intent(getApplicationContext(), EmergencyService.class);
        startService(emergencyServiceIntent);
        emergencyServiceConnection = new CommandServiceConnection();
        bindService(emergencyServiceIntent, emergencyServiceConnection, Context.BIND_NOT_FOREGROUND);

    }

    private void startNetworkService() {
        Log.d(TAG, "startNetworkService");

        Intent cloudServiceIntent = new Intent(getApplicationContext(), CloudService.class);
        startService(cloudServiceIntent);
        cloudServiceConnection = new CommandServiceConnection();
        bindService(cloudServiceIntent, cloudServiceConnection, Context.BIND_NOT_FOREGROUND);

    }

    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();

        if (serviceClassName.equals(UasCommunicationService.UasCommunicationServiceBinder.class.getName())) {
            uasCommunicationService = ((UasCommunicationService.UasCommunicationServiceBinder) service).getService();
        } else if (serviceClassName.equals(LocationService.LocationServiceBinder.class.getName())) {
            locationService = ((LocationService.LocationServiceBinder) service).getService();
        } else if (serviceClassName.equals(EmergencyService.EmergencyServiceBinder.class.getName())) {
            emergencyService = ((EmergencyService.EmergencyServiceBinder) service).getService();
        } else if (serviceClassName.equals(CloudService.CloudServiceBinder.class.getName())) {
            cloudService = ((CloudService.CloudServiceBinder) service).getService();
        } else {
            Log.w(TAG, "Unrecognized service class name: " + serviceClassName);
        }

        broadcastIfServicesReady();
    }

    protected void broadcastIfServicesReady() {
        Log.d(TAG, "broadcastIfServicesReady");

        if ((uasCommunicationService != null) && (locationService != null) && (emergencyService != null) && (cloudService != null)) {
            sendBroadcast(new Intent(ACTION_SERVICES_READY));
        }
    }

    private void stopServices() {
        Log.d(TAG, "stopServices");

        unbindService(uasCommunicationServiceConnection);
        uasCommunicationService = null;
        unbindService(locationServiceConnection);
        locationService = null;
        unbindService(emergencyServiceConnection);
        emergencyService = null;
        unbindService(cloudServiceConnection);
        cloudService = null;
    }

    protected void startWifiP2pScanning() {
        Log.d(TAG, "startScanning");
        uasCommunicationService.startScanning();
    }

    protected void connectToWifiP2pDevice(WifiP2pDevice device) {
        Log.d(TAG, "connectToDevice: device.toString()=" + device.toString());
        uasCommunicationService.connectToDevice(device);
    }

    protected static void notifyWifiP2pConnected(Context context) {
        Log.d(TAG, "notifyWifiP2pConnected");

        context.sendBroadcast(new Intent(ACTION_WIFI_P2P_CONNECTED));
    }

    protected static void notifyLocationCalibrationComplete(Context context) {
        Log.d(TAG, "notifyLocationCalibrationComplete");

        context.sendBroadcast(new Intent(ACTION_LOCATION_CALIBRATION_COMPLETE));
    }

    protected static void notifyNewWaypointAvailable(Context context) {
        Log.d(TAG, "notifyNewWaypointAvailable");

        context.sendBroadcast(new Intent(ACTION_NEW_WAYPOINT));
    }

    protected static void notifyNewUasImageAvailable(Context context) {
        Log.d(TAG, "notifyNewUasImageAvailable");

        context.sendBroadcast(new Intent(ACTION_NEW_UAS_IMAGE));
    }

    protected Bitmap getNewImage(){

        return uasCommunicationService.getNewImage() == null ? null: uasCommunicationService.getNewImage();
    }

    private void handleNewWaypoint() {
        Log.d(TAG, "handleNewWaypoint");

        Location waypoint = locationService.getLastWaypointLocation();
        uasCommunicationService.sendWaypoint(waypoint);
    }

    protected void handleCommandHhmdEmergency() {
        Log.d(TAG, "handleCommandHhmdEmergency");

        String sessionCloudLink = cloudService.getSessionCloudLink();
        Location lastHhmdLocation = locationService.getLastHhmdLocation();
        emergencyService.startEmergency(lastHhmdLocation, sessionCloudLink);
        uasCommunicationService.startEmergency();
    }

    protected void handleCommandHhmdLight(boolean lightOnOff) {
        Log.d(TAG, "handleCommandHhmdLight: lightOnOff=" + lightOnOff);

        uasCommunicationService.setLightOnOff(lightOnOff);
    }

    protected void handleCommandHhmdUasHeightUp() {
        Log.d(TAG, "handleCommandHhmdUasHeightUp");

        locationService.incrementHeightOffset();
    }

    protected void handleCommandHhmdUasHeightDown() {
        Log.d(TAG, "handleCommandHhmdUasHeightDown");

        locationService.decrementHeightOffset();
    }

    protected void handleCommandHhmdSessionStart() {
        Log.d(TAG, "handleCommandHhmdSessionStart");

        uasCommunicationService.startSession();
        cloudService.startSession();
        locationService.startSession();
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd");

        uasCommunicationService.stopSession();
        locationService.stopSession();
    }

    private void handleNewUasLocation() {
        Log.d(TAG, "handleNewUasLocation");

        Location uasLocation = uasCommunicationService.getNewUasLocation();
        locationService.pushUasLocation(uasLocation);
    }

    private void handleNewUasImage() {
        Log.d(TAG, "handleNewUasImage");

        Bitmap bitmap = uasCommunicationService.getNewImage();
        cloudService.saveImage(bitmap);
    }

    private class CommandServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case ACTION_NEW_WAYPOINT:
                        handleNewWaypoint();
                        break;
                    case ACTION_NEW_UAS_LOCATION:
                        handleNewUasLocation();
                        break;
                    case ACTION_NEW_UAS_IMAGE:
                        handleNewUasImage();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }

    public class CommandServiceBinder extends Binder {
        CommandService getService() {
            return CommandService.this;
        }
    }

    protected class CommandServiceConnection implements ServiceConnection {
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
