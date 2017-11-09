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
 * application.
 */

public class CommandService extends Service {
    //Broadcasts to notify UI of state change
    protected static final String ACTION_SERVICES_STATE_CHANGED = "com.helpfromabove.helpfromabove.action.ACTION_SERVICES_STATE_CHANGED";
    protected static final String ACTION_WIFI_P2P_STATE_CHANGED = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_STATE_CHANGED";
    protected static final String ACTION_LOCATION_STATE_CHANGED = "com.helpfromabove.helpfromabove.action.ACTION_LOCATION_STATE_CHANGED";
    protected static final String ACTION_SESSION_STATE_CHANGED = "com.helpfromabove.helpfromabove.action.ACTION_SESSION_STATE_CHANGED";
    protected static final String ACTION_NEW_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_IMAGE";
    protected static final String ACTION_NEW_WAYPOINT = "com.helpfromabove.helpfromabove.action.ACTION_NEW_WAYPOINT";
    protected static final String ACTION_NEW_UAS_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_LOCATION";
    protected static final String ACTION_NEW_HHMD_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_HHMD_LOCATION";

    // Broadcasts for other services to use
    private static final String ACTION_WIFI_P2P_DISCONNECTED = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_DISCONNECTED";
    private static final String ACTION_WIFI_P2P_CONNECTING_TO_UASC = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_CONNECTING_TO_UASC";
    private static final String ACTION_WIFI_P2P_WAITING_FOR_UASC = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_WAITING_FOR_UASC";
    private static final String ACTION_WIFI_P2P_CONNECTING_FROM_UASC = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_CONNECTING_FROM_UASC";
    private static final String ACTION_WIFI_P2P_CONNECTED = "com.helpfromabove.helpfromabove.action.ACTION_WIFI_P2P_CONNECTED";
    private static final String ACTION_LOCATION_CALIBRATING = "com.helpfromabove.helpfromabove.action.ACTION_LOCATION_CALIBRATING";
    private static final String ACTION_LOCATION_HHMD_CALIBRATION_COMPLETE = "com.helpfromabove.helpfromabove.action.ACTION_LOCATION_HHMD_CALIBRATION_COMPLETE";
    private static final String ACTION_LOCATION_UASC_CALIBRATION_COMPLETE = "com.helpfromabove.helpfromabove.action.ACTION_LOCATION_UASC_CALIBRATION_COMPLETE";
    private static final String ACTION_LOCATION_CALIBRATION_COMPLETE = "com.helpfromabove.helpfromabove.action.ACTION_LOCATION_CALIBRATION_COMPLETE";
    private static final String ACTION_SESSION_EMERGENCY_MESSAGES_SENT = "com.helpfromabove.helpfromabove.action.ACTION_SESSION_EMERGENCY_MESSAGES_SENT";

    protected static final String ACTION_SKIPPED_WIFI_CONNECTION = "com.helpfromabove.helpfromabove.action.ACTION_SKIPPED_WIFI_CONNECTION";

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

    private static boolean startAllSessions = false;
    private static int receivedImagesCount = 0;

    private CommandServiceBroadcastReceiver commandServiceBroadcastReceiver;

    protected enum ServicesState {
        SERVICES_STOPPED,
        SERVICES_STARTING,
        SERVICES_STARTED,
    }

    protected enum WifiP2pState {
        WIFI_P2P_DISCONNECTED,
        WIFI_P2P_CONNECTING_TO_UASC,
        WIFI_P2P_WAITING_FOR_UASC,
        WIFI_P2P_CONNECTING_FROM_UASC,
        WIFI_P2P_CONNECTED,
    }

    protected enum LocationState {
        LOCATION_NOT_CALIBRATED,
        LOCATION_CALIBRATING,
        LOCATION_HHMD_CALIBRATED,
        LOCATION_UASC_CALIBRATED,
        LOCATION_CALIBRATED,
    }

    protected enum SessionState {
        SESSION_STARTING,
        SESSION_RUNNING,
        SESSION_EMERGENCY_STARTED,
        SESSION_EMERGENCY_MESSAGES_SENT,
        SESSION_STOPPING,
        SESSION_STOPPED,
    }

    protected class State {
        private ServicesState servicesState;
        private WifiP2pState wifiP2pState;
        private LocationState locationState;
        private SessionState sessionState;

        State(ServicesState servicesState, WifiP2pState wifiP2pState, LocationState locationState, SessionState sessionState) {
            this.servicesState = servicesState;
            this.wifiP2pState = wifiP2pState;
            this.locationState = locationState;
            this.sessionState = sessionState;
        }

        void setServicesState(ServicesState servicesState) {
            this.servicesState = servicesState;
            CommandService.notifyServicesStateChanged(getApplicationContext());
        }

        void setWifiP2pState(WifiP2pState wifiP2pState) {
            this.wifiP2pState = wifiP2pState;
            CommandService.notifyWifiP2pStateChanged(getApplicationContext());
        }

        void setLocationState(LocationState locationState) {
            this.locationState = locationState;
            CommandService.notifyLocationStateChanged(getApplicationContext());
        }

        void setSessionState(SessionState sessionState) {
            this.sessionState = sessionState;
            CommandService.notifySessionStateChanged(getApplicationContext());
        }

        ServicesState getServicesState() {
            return servicesState;
        }

        WifiP2pState getWifiP2pState() {
            return wifiP2pState;
        }

        LocationState getLocationState() {
            return locationState;
        }

        SessionState getSessionState() {
            return sessionState;
        }
    }

    private State state;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        state = new State(ServicesState.SERVICES_STOPPED, WifiP2pState.WIFI_P2P_DISCONNECTED, LocationState.LOCATION_NOT_CALIBRATED, SessionState.SESSION_STOPPED);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_WIFI_P2P_DISCONNECTED);
        intentFilter.addAction(ACTION_WIFI_P2P_CONNECTING_TO_UASC);
        intentFilter.addAction(ACTION_WIFI_P2P_WAITING_FOR_UASC);
        intentFilter.addAction(ACTION_WIFI_P2P_CONNECTING_FROM_UASC);
        intentFilter.addAction(ACTION_WIFI_P2P_CONNECTED);
        intentFilter.addAction(ACTION_LOCATION_CALIBRATING);
        intentFilter.addAction(ACTION_LOCATION_HHMD_CALIBRATION_COMPLETE);
        intentFilter.addAction(ACTION_LOCATION_UASC_CALIBRATION_COMPLETE);
        intentFilter.addAction(ACTION_LOCATION_CALIBRATION_COMPLETE);
        intentFilter.addAction(ACTION_SESSION_EMERGENCY_MESSAGES_SENT);
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

    protected State getState() {
        return state;
    }

    private void startServices() {
        state.setServicesState(ServicesState.SERVICES_STARTING);
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

        if ((uasCommunicationService != null) && (locationService != null) && (emergencyService != null) && (cloudService != null)) {
            state.setServicesState(ServicesState.SERVICES_STARTED);
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

        state.setServicesState(ServicesState.SERVICES_STOPPED);
    }

    protected void startWifiP2pScanning() {
        Log.d(TAG, "startScanning");
        uasCommunicationService.startScanning();
    }

    protected void connectToWifiP2pDevice(WifiP2pDevice device) {
        Log.d(TAG, "connectToDevice: device.toString()=" + device.toString());
        uasCommunicationService.connectToDevice(device);
    }

    private static void notifyServicesStateChanged(Context context) {
        Log.d(TAG, "notifyServicesStateChanged");

        context.sendBroadcast(new Intent(ACTION_SERVICES_STATE_CHANGED));
    }

    private static void notifyWifiP2pStateChanged(Context context) {
        Log.d(TAG, "notifyWifiP2pStateChanged");

        context.sendBroadcast(new Intent(ACTION_WIFI_P2P_STATE_CHANGED));
    }

    private static void notifyLocationStateChanged(Context context) {
        Log.d(TAG, "notifyLocationStateChanged");

        context.sendBroadcast(new Intent(ACTION_LOCATION_STATE_CHANGED));
    }

    private static void notifySessionStateChanged(Context context) {
        Log.d(TAG, "notifySessionStateChanged");

        context.sendBroadcast(new Intent(ACTION_SESSION_STATE_CHANGED));
    }

    protected static void notifyWifiP2pConnectingToUasc(Context context) {
        Log.d(TAG, "notifyWifiP2pConnectingToUasc");

        context.sendBroadcast(new Intent(ACTION_WIFI_P2P_CONNECTING_TO_UASC));
    }

    protected static void notifyWifiP2pWaitingForUasc(Context context) {
        Log.d(TAG, "notifyWifiP2pWaitingForUasc");

        context.sendBroadcast(new Intent(ACTION_WIFI_P2P_WAITING_FOR_UASC));
    }

    protected static void notifyWifiP2pConnectingFromUasc(Context context) {
        Log.d(TAG, "notifyWifiP2pConnectingFromUasc");

        context.sendBroadcast(new Intent(ACTION_WIFI_P2P_CONNECTING_FROM_UASC));
    }

    protected static void notifyWifiP2pConnected(Context context) {
        Log.d(TAG, "notifyWifiP2pConnected");

        context.sendBroadcast(new Intent(ACTION_WIFI_P2P_CONNECTED));
    }

    protected static void notifyLocationCalibrating(Context context) {
        Log.d(TAG, "notifyLocationCalibrating");

        context.sendBroadcast(new Intent(ACTION_LOCATION_CALIBRATING));

    }

    protected static void notifyLocationHhmdCalibrationComplete(Context context) {
        Log.d(TAG, "notifyLocationHhmdCalibrationComplete");

        startAllSessions = true;
        context.sendBroadcast(new Intent(ACTION_LOCATION_HHMD_CALIBRATION_COMPLETE));
    }

    protected static void notifyLocationUascCalibrationComplete(Context context) {
        Log.d(TAG, "notifyLocationUascCalibrationComplete");

        context.sendBroadcast(new Intent(ACTION_LOCATION_UASC_CALIBRATION_COMPLETE));
    }

    protected static void notifyLocationCalibrationComplete(Context context) {
        Log.d(TAG, "notifyLocationCalibrationComplete");

        context.sendBroadcast(new Intent(ACTION_LOCATION_CALIBRATION_COMPLETE));
    }

    protected static void notifyEmergencyMessagesSent(Context context) {
        Log.d(TAG, "notifyEmergencyMessagesSent");

        context.sendBroadcast(new Intent(ACTION_SESSION_EMERGENCY_MESSAGES_SENT));
    }

    protected static void notifyNewWaypointAvailable(Context context) {
        Log.d(TAG, "notifyNewWaypointAvailable");

        context.sendBroadcast(new Intent(ACTION_NEW_WAYPOINT));
    }

    protected static void notifyNewUasImageAvailable(Context context) {
        Log.d(TAG, "notifyNewUasImageAvailable");

        receivedImagesCount++;
        context.sendBroadcast(new Intent(ACTION_NEW_UAS_IMAGE));
    }

    protected static void notifySkippedWifiConnection(Context context){

        context.sendBroadcast(new Intent(ACTION_SKIPPED_WIFI_CONNECTION));
    }

    protected Bitmap getNewImage() {

        return uasCommunicationService.getNewImage();
    }

    private void handleNewWaypoint() {
        Log.d(TAG, "handleNewWaypoint");

        Location waypoint = locationService.getLastWaypointLocation();
        uasCommunicationService.sendWaypoint(waypoint);
    }

    protected void handleCommandHhmdEmergency() {
        Log.d(TAG, "handleCommandHhmdEmergency");

        state.setSessionState(SessionState.SESSION_EMERGENCY_STARTED);

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

        state.setSessionState(SessionState.SESSION_STARTING);

        uasCommunicationService.startSession();
        cloudService.startSession();
        locationService.startSession();
    }

    private void handleLocationHhmdCalibrationComplete() {
        if (state.getLocationState() == LocationState.LOCATION_CALIBRATING) {
            state.setLocationState(LocationState.LOCATION_HHMD_CALIBRATED);
            uasCommunicationService.sendStartSession();
        } else if (state.getLocationState() == LocationState.LOCATION_UASC_CALIBRATED) {
            notifyLocationCalibrationComplete(getApplicationContext());
        }
    }

    private void handleLocationUascCalibrationComplete() {
        if (state.getLocationState() == LocationState.LOCATION_CALIBRATING) {
            state.setLocationState(LocationState.LOCATION_UASC_CALIBRATED);
        } else if (state.getLocationState() == LocationState.LOCATION_HHMD_CALIBRATED) {
            notifyLocationCalibrationComplete(getApplicationContext());
        }
    }


    private void handleLocationCalibrationComplete() {
        Log.d(TAG, "handleLocationCalibrationComplete");

        state.setLocationState(LocationState.LOCATION_CALIBRATED);

        locationService.onLocationCalibrationComplete();

        if (startAllSessions) {
            if (receivedImagesCount == cloudService.getUploadCount()) {
                receivedImagesCount = 0;
                uasCommunicationService.onLocationCalibrationComplete();
                cloudService.onLocationCalibrationComplete();
            } else {
                Log.d(TAG, "Still uploading images form the last session");
            }
        }

        state.setSessionState(SessionState.SESSION_RUNNING);
    }

    private void handleSessionEmergencyMessagesSent() {
        state.setSessionState(SessionState.SESSION_EMERGENCY_MESSAGES_SENT);
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd");

        state.setSessionState(SessionState.SESSION_STOPPING);

        uasCommunicationService.stopSession();
        locationService.stopSession();

        state.setSessionState(SessionState.SESSION_STOPPED);
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
            if (action != null) {
                switch (action) {
                    case ACTION_WIFI_P2P_DISCONNECTED:
                        state.setWifiP2pState(WifiP2pState.WIFI_P2P_DISCONNECTED);
                        break;
                    case ACTION_WIFI_P2P_CONNECTING_TO_UASC:
                        state.setWifiP2pState(WifiP2pState.WIFI_P2P_CONNECTING_TO_UASC);
                        break;
                    case ACTION_WIFI_P2P_WAITING_FOR_UASC:
                        state.setWifiP2pState(WifiP2pState.WIFI_P2P_WAITING_FOR_UASC);
                        break;
                    case ACTION_WIFI_P2P_CONNECTING_FROM_UASC:
                        state.setWifiP2pState(WifiP2pState.WIFI_P2P_CONNECTING_FROM_UASC);
                        break;
                    case ACTION_WIFI_P2P_CONNECTED:
                        state.setWifiP2pState(WifiP2pState.WIFI_P2P_CONNECTED);
                        break;
                    case ACTION_LOCATION_CALIBRATING:
                        state.setLocationState(LocationState.LOCATION_CALIBRATING);
                        break;
                    case ACTION_LOCATION_HHMD_CALIBRATION_COMPLETE:
                        handleLocationHhmdCalibrationComplete();
                        break;
                    case ACTION_LOCATION_UASC_CALIBRATION_COMPLETE:
                        handleLocationUascCalibrationComplete();
                        break;
                    case ACTION_LOCATION_CALIBRATION_COMPLETE:
                        handleLocationCalibrationComplete();
                        break;
                    case ACTION_SESSION_EMERGENCY_MESSAGES_SENT:
                        handleSessionEmergencyMessagesSent();
                        break;
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

    class CommandServiceBinder extends Binder {
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
