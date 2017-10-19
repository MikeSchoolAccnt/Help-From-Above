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
    protected static final String ACTION_UI_SERVICES_READY = "com.helpfromabove.helpfromabove.action.ACTION_UI_SERVICES_READY";
    protected static final String ACTION_UI_WIFI_P2P_CONNECTED = "com.helpfromabove.helpfromabove.action.ACTION_UI_WIFI_P2P_CONNECTED";
    protected static final String ACTION_UI_NEW_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_UI_NEW_IMAGE";
    protected static final String ACTION_NEW_WAYPOINT_AVAILABLE = "com.helpfromabove.helpfromabove.command.ACTION_NEW_WAYPOINT_AVAILABLE";
    protected static final String ACTION_NEW_UAS_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_LOCATION";
    protected static final String ACTION_NEW_HHMD_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_HHMD_LOCATION";
    protected static final String ACTION_REQUEST_LAST_IMAGE_FILENAME = "com.helpfromabove.helpfromabove.action.ACTION_REQUEST_LAST_IMAGE_FILENAME";
    protected static final String EXTRA_IMAGE_FILE_NAME = "com.helpfromabove.helpfromabove.extra.EXTRA_IMAGE_FILE_NAME";
    protected static final String EXTRA_LOCATION = "com.helpfromabove.helpfromabove.extra.EXTRA_LOCATION";
    protected static final String COMMAND_UAS_IMAGE = "com.helpfromabove.helpfromabove.command.COMMAND_UAS_IMAGE";
    protected static final String COMMAND_UAS_LOCATION = "com.helpfromabove.helpfromabove.command.COMMAND_UAS_LOCATION";

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
        intentFilter.addAction(ACTION_NEW_WAYPOINT_AVAILABLE);
        intentFilter.addAction(ACTION_REQUEST_LAST_IMAGE_FILENAME);
        intentFilter.addAction(COMMAND_UAS_IMAGE);
        intentFilter.addAction(COMMAND_UAS_LOCATION);
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

    protected void broadcastIfServicesReady() {
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

    /******************************************************
     * In the future, we might want all static methods to
     * only send action intents (without extras, or very
     * little data [like ints or strings]). That way when
     * the dynamic methods are called, they have to get
     * the latest data values from the service instances.
     * That would reduce the amount of data sent, and
     * restrictions on size of data sent through intents
     ******************************************************/

    protected static void notifyUiWifiP2pConnected(Context contest) {
        Log.d(TAG, "notifyUiWifiP2pConnected");

        contest.sendBroadcast(new Intent(ACTION_UI_WIFI_P2P_CONNECTED));
    }

    protected static void sendUasWaypoint(Context context) {
        Log.d(TAG, "sendUasWaypoint");

        context.sendBroadcast(new Intent(ACTION_NEW_WAYPOINT_AVAILABLE));
    }

    protected static void notifyUiNewImageAvailable(Context context) {
        Log.d(TAG, "notifyUiNewImageAvailable");

        context.sendBroadcast(new Intent(ACTION_UI_NEW_IMAGE));
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

    public void addSessionImageFileName(String imageFileName) {
        Log.d(TAG, "addSessionImageFileName");

        mImageFileNamesStack.push(imageFileName);
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
// TODO: Figure out how this will work
//        String filename = getDateTime() + ".jpg";
//        saveImage(filename);
//        uploadImage(filename);
//        addSessionImageFileName(filename);
//        sendNewImageIntent();
    }

    private void saveImage(String filename) {
        Log.d(TAG, "saveImage");

        try {
            FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE);

            // This is for local image testing. Modify once local image testing is complete
            outputStream.write(imageBytes.get(imageDebugCounter % 3));

            outputStream.close();
        } catch (IOException iOE) {
            Log.e(TAG, "handleCommandUasImage: IOException: " + iOE.getMessage(), iOE);
        }
    }

    private class CommandServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case ACTION_NEW_WAYPOINT_AVAILABLE:
                        handleSendWaypoint();
                        break;
                    case COMMAND_UAS_LOCATION:
                        final Location uasLocation = intent.getExtras().getParcelable(EXTRA_LOCATION);
                        handleCommandUasLocation(uasLocation);
                        break;
                    case COMMAND_UAS_IMAGE:
                        handleCommandUasImage();
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
