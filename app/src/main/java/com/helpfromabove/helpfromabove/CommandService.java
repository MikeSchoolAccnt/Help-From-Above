package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by Caleb Smith on 5/13/2017.
 * <p>
 * The CommandService is the model of the application.
 * Its purpose is to broadcast intents to visible activities, store
 * objects that need to be persistent throughout the life of the
 * application, and perform calculations on locations for waypoints.
 */

public class CommandService extends Service {
    protected static final String ACTION_REQUEST_SERVICES_READY = "com.helpfromabove.helpfromabove.action.ACTION_REQUEST_SERVICES_READY";
    protected static final String ACTION_UI_SERVICES_READY = "com.helpfromabove.helpfromabove.action.ACTION_UI_SERVICES_READY";
    protected static final String ACTION_UI_WIFI_P2P_CONNECTED = "com.helpfromabove.helpfromabove.action.ACTION_UI_WIFI_P2P_CONNECTED";
    protected static final String ACTION_NEW_WAYPOINT_AVAILABLE = "com.helpfromabove.helpfromabove.command.ACTION_NEW_WAYPOINT_AVAILABLE";

    protected static final String ACTION_NEW_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_IMAGE";
    protected static final String ACTION_NEW_UAS_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_LOCATION";
    protected static final String ACTION_NEW_HHMD_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_HHMD_LOCATION";
    protected static final String ACTION_REQUEST_LAST_IMAGE_FILENAME = "com.helpfromabove.helpfromabove.action.ACTION_REQUEST_LAST_IMAGE_FILENAME";
    protected static final String EXTRA_LIGHT_ON_OFF = "com.helpfromabove.helpfromabove.extra.EXTRA_LIGHT_ON_OFF";
    protected static final String EXTRA_IMAGE_FILE_NAME = "com.helpfromabove.helpfromabove.extra.EXTRA_IMAGE_FILE_NAME";
    protected static final String EXTRA_LOCATION = "com.helpfromabove.helpfromabove.extra.EXTRA_LOCATION";
    protected static final String COMMAND_HHMD_EMERGENCY = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_EMERGENCY";
    protected static final String COMMAND_HHMD_LIGHT = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_LIGHT";
    protected static final String COMMAND_HHMD_UAS_HEIGHT_UP = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_UAS_HEIGHT_UP";
    protected static final String COMMAND_HHMD_UAS_HEIGHT_DOWN = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_UAS_HEIGHT_DOWN";
    protected static final String COMMAND_HHMD_SESSION_START = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_SESSION_START";
    protected static final String COMMAND_HHMD_SESSION_END = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_SESSION_END";
    protected static final String COMMAND_UAS_IMAGE = "com.helpfromabove.helpfromabove.command.COMMAND_UAS_IMAGE";
    protected static final String COMMAND_UAS_LOCATION = "com.helpfromabove.helpfromabove.command.COMMAND_UAS_LOCATION";
    protected static final String SETTING_CHANGE_START_HEIGHT = "com.helpfromabove.helpfromabove.setting.SETTING_CHANGE_START_HEIGHT";

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
    private Stack<String> mImageFileNamesStack = new Stack<>();

    // This is for local image testing. Remove once local image testing is complete
    private int imageDebugCounter = 0;
    private ArrayList<byte[]> imageBytes = new ArrayList<>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        mImageFileNamesStack = new Stack<>();

        intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_REQUEST_SERVICES_READY);
        intentFilter.addAction(ACTION_NEW_WAYPOINT_AVAILABLE);
        intentFilter.addAction(ACTION_REQUEST_LAST_IMAGE_FILENAME);
        intentFilter.addAction(COMMAND_HHMD_EMERGENCY);
        intentFilter.addAction(COMMAND_HHMD_LIGHT);
        intentFilter.addAction(COMMAND_HHMD_UAS_HEIGHT_UP);
        intentFilter.addAction(COMMAND_HHMD_UAS_HEIGHT_DOWN);
        intentFilter.addAction(COMMAND_HHMD_SESSION_START);
        intentFilter.addAction(COMMAND_HHMD_SESSION_END);
        intentFilter.addAction(COMMAND_UAS_IMAGE);
        intentFilter.addAction(COMMAND_UAS_LOCATION);
        intentFilter.addAction(SETTING_CHANGE_START_HEIGHT);
        commandServiceBroadcastReceiver = new CommandServiceBroadcastReceiver();
        registerReceiver(commandServiceBroadcastReceiver, intentFilter);

        startServices();

        // This is for local image testing. Remove once local image testing is complete
        Bitmap bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4240);
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baoStream);
        imageBytes.add(baoStream.toByteArray());
        bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4366);
        baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baoStream);
        imageBytes.add(baoStream.toByteArray());
        bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4491);
        baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baoStream);
        imageBytes.add(baoStream.toByteArray());
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

    private void broadcastIfServicesReady() {
        Log.d(TAG, "broadcastIfServicesReady");

        if ((uasCommunicationService != null) && (locationService != null) && (emergencyService != null) && (cloudService != null)) {
            sendBroadcast(new Intent(ACTION_UI_SERVICES_READY));
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

    protected static void notifyUiWifiP2pConnected(Context contest) {
        Log.d(TAG, "notifyUiWifiP2pConnected");

        contest.sendBroadcast(new Intent(ACTION_UI_WIFI_P2P_CONNECTED));
    }

    protected static void sendUasWaypoint(Context context) {
        Log.d(TAG, "sendUasWaypoint");

        context.sendBroadcast(new Intent(ACTION_NEW_WAYPOINT_AVAILABLE));
    }

    protected Bitmap getNewImage(){
        return uasCommunicationService.getNewImage();
    }

    private void handleSendWaypoint() {
        Log.d(TAG, "handleSendWaypoint");

        Location waypoint = locationService.getLastWaypointLocation();
        uasCommunicationService.sendWaypoint(waypoint);
    }

    public String getLastSessionImageFileName() {
        Log.d(TAG, "getLastSessionImageFileName");

        String imageFileName;
        if (!mImageFileNamesStack.empty()) {
            imageFileName = mImageFileNamesStack.peek();
        } else {
            Log.d(TAG, "getLastSessionImageFileName: mImageFileNamesStack is empty.");

            imageFileName = null;
        }

        Log.d(TAG, "getLastSessionImageFileName: imageFileName=" + imageFileName);
        return imageFileName;
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

        // This is for local image testing. Remove once local image testing is complete
        if (imageDebugCounter >= 2) {
            Log.d(TAG, "handleCommandHhmdLight: incoming image example");

            handleCommandUasImage();
        }
        imageDebugCounter++;
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

    private void handleCommandUasLocation(Location uasLocation) {
        Log.d(TAG, "handleCommandUasLocation: uasLocation=" + uasLocation);
        locationService.pushUasLocation(uasLocation);
    }

    private void handleCommandUasImage() {
        Log.d(TAG, "handleCommandUasImage");

        // TODO: Add method for getting the most recent image data from the UasCOmmunicationService class.
        Bitmap bitmap = null;// = uasCommunicationService.getImageData();
        cloudService.saveImage(bitmap);
    }

    private void handleSettingChangeStartHeight() {
        Log.d(TAG, "handleSettingChangeStartHeight");
    }

    private void sendNewImageIntent() {
        Log.d(TAG, "sendNewImageIntent");

        Intent newImageIntent = new Intent(ACTION_NEW_UAS_IMAGE);
        newImageIntent.putExtra(EXTRA_IMAGE_FILE_NAME, getLastSessionImageFileName());
        sendBroadcast(newImageIntent);
    }

    private class CommandServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case ACTION_REQUEST_SERVICES_READY:
                        broadcastIfServicesReady();
                        break;
                    case ACTION_NEW_WAYPOINT_AVAILABLE:
                        handleSendWaypoint();
                        break;
                    case ACTION_REQUEST_LAST_IMAGE_FILENAME:
                        sendNewImageIntent();
                        break;
                    case COMMAND_HHMD_EMERGENCY:
                        handleCommandHhmdEmergency();
                        break;
                    case COMMAND_HHMD_LIGHT:
                        final boolean lightOnOff = intent.getBooleanExtra(EXTRA_LIGHT_ON_OFF, true);
                        handleCommandHhmdLight(lightOnOff);
                        break;
                    case COMMAND_HHMD_UAS_HEIGHT_UP:
                        handleCommandHhmdUasHeightUp();
                        break;
                    case COMMAND_HHMD_UAS_HEIGHT_DOWN:
                        handleCommandHhmdUasHeightDown();
                        break;
                    case COMMAND_HHMD_SESSION_START:
                        handleCommandHhmdSessionStart();
                        break;
                    case COMMAND_HHMD_SESSION_END:
                        handleCommandHhmdSessionEnd();
                        break;
                    case COMMAND_UAS_LOCATION:
                        final Location uasLocation = intent.getExtras().getParcelable(EXTRA_LOCATION);
                        handleCommandUasLocation(uasLocation);
                        break;
                    case COMMAND_UAS_IMAGE:
                        handleCommandUasImage();
                        break;
                    case SETTING_CHANGE_START_HEIGHT:
                        handleSettingChangeStartHeight();
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
