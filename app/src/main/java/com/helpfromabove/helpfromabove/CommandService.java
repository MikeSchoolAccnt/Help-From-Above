package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.OneDrive;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

/**
 * Created by Caleb Smith on 5/13/2017.
 * <p>
 * The CommandService is the model of the application.
 * Its purpose is to broadcast intents to visible activities, store
 * objects that need to be persistent throughout the life of the
 * application, and perform calculations on locations for waypoints.
 */

public class CommandService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected static final String ACTION_UI_SERVICES_READY = "com.helpfromabove.helpfromabove.action.ACTION_UI_SERVICES_READY";
    protected static final String ACTION_UI_WIFI_P2P_CONNECTED = "com.helpfromabove.helpfromabove.action.ACTION_UI_WIFI_P2P_CONNECTED";
    protected static final String SEND_WAYPOINT = "com.helpfromabove.helpfromabove.command.SEND_WAYPOINT";

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
    protected static final String SETTING_CHANGE_CLOUD = "com.helpfromabove.helpfromabove.setting.SETTING_CHANGE_CLOUD";
    protected static final String SETTING_CHANGE_START_HEIGHT = "com.helpfromabove.helpfromabove.setting.SETTING_CHANGE_START_HEIGHT";
    protected static final String EXTRA_CLOUD_TYPE = "com.helpfromabove.helpfromabove.extra.EXTRA_CLOUD_TYPE";
    protected static final String CONSTANT_CLOUD_DROPBOX = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_DROPBOX";
    protected static final String CONSTANT_CLOUD_GOOGLE_DRIVE = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_GOOGLE_DRIVE";
    protected static final String CONSTANT_CLOUD_ONE_DRIVE = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_ONE_DRIVE";

    //App keys needed for CloudRail and other Cloud Services
    private final static String CLOUDRAIL_LICENSE_KEY = "59c031993d7042599787c8a8";
    private final static String DROPBOX_APP_KEY = "th6i7dbzxmnzbu5";
    private final static String DROPBOX_APP_SECRET = "22vq1tpd68tm28l";
    //Still need application keys for GoogleDrive and OneDrive
    private final static String GOOGLE_DRIVE_APP_KEY = "";
    private final static String GOOGLE_DRIVE_APP_SECRET = "";
    private final static String ONE_DRIVE_APP_KEY = "8c273966-c62c-48b0-8500-d42b849bbf18";
    private final static String ONE_DRIVE_APP_SECRET = "3tGij2AmL0dGx7pHkukgK9o";

    private final static String TAG = "CommandService";

    private final IBinder mBinder = new CommandServiceBinder();
    ServiceConnection uasCommunicationServiceConnection;
    UasCommunicationService uasCommunicationService;
    ServiceConnection locationServiceConnection;
    LocationService locationService;
    ServiceConnection emergencyServiceConnection;
    EmergencyService emergencyService;


    private final String CLOUD_APP_FOLDER = "/" + "Help_From_Above";
    private String cloudSessionFolder;
    private CommandServiceBroadcastReceiver commandServiceBroadcastReceiver;
    private IntentFilter intentFilter;
    private Stack<String> mImageFileNamesStack = new Stack<>();
    private CloudStorage cloudStorage;

    // This is for local image testing. Remove once local image testing is complete
    private int imageDebugCounter = 0;
    private ArrayList<byte[]> imageBytes = new ArrayList<>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        CloudRail.setAppKey(CLOUDRAIL_LICENSE_KEY);

        mImageFileNamesStack = new Stack<>();

        intentFilter = new IntentFilter();
        intentFilter.addAction(SEND_WAYPOINT);
        intentFilter.addAction(ACTION_REQUEST_LAST_IMAGE_FILENAME);
        intentFilter.addAction(COMMAND_HHMD_EMERGENCY);
        intentFilter.addAction(COMMAND_HHMD_LIGHT);
        intentFilter.addAction(COMMAND_HHMD_UAS_HEIGHT_UP);
        intentFilter.addAction(COMMAND_HHMD_UAS_HEIGHT_DOWN);
        intentFilter.addAction(COMMAND_HHMD_SESSION_START);
        intentFilter.addAction(COMMAND_HHMD_SESSION_END);
        intentFilter.addAction(COMMAND_UAS_IMAGE);
        intentFilter.addAction(COMMAND_UAS_LOCATION);
        intentFilter.addAction(SETTING_CHANGE_CLOUD);
        intentFilter.addAction(SETTING_CHANGE_START_HEIGHT);
        commandServiceBroadcastReceiver = new CommandServiceBroadcastReceiver();
        registerReceiver(commandServiceBroadcastReceiver, intentFilter);

        startServices();

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

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

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
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
        Log.d(TAG, "startNetworkService: NOT IMPLEMENTED!");
    }

    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();
        Log.d(TAG, "setConnectedService: check if the class name is the class of the networkServiceBinder and set networkService reference here");
        if (serviceClassName.equals(UasCommunicationService.UasCommunicationServiceBinder.class.getName())) {
            uasCommunicationService = ((UasCommunicationService.UasCommunicationServiceBinder) service).getService();
        } else if (serviceClassName.equals(LocationService.LocationServiceBinder.class.getName())) {
            locationService = ((LocationService.LocationServiceBinder) service).getService();
        } else if (serviceClassName.equals(EmergencyService.EmergencyServiceBinder.class.getName())) {
            emergencyService = ((EmergencyService.EmergencyServiceBinder) service).getService();
        } else {
            Log.w(TAG, "Unrecognized service class name: " + serviceClassName);
        }
        onServiceConnected();
    }

    private void onServiceConnected() {
        Log.d(TAG, "onServiceConnected");
        Log.d(TAG, "onServiceConnected: check if networkService is null here");
        if ((uasCommunicationService != null) && (locationService != null) && (emergencyService != null)) {
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
        Log.d(TAG, "stopServices: unbind networkServiceConnection and set networkService reference to null here");
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

    protected static void sendUasWaypoint(Context context, Location waypoint) {
        Log.d(TAG, "sendUasWaypoint");

        Intent sendWaypointIntent = new Intent(SEND_WAYPOINT);
        sendWaypointIntent.putExtra(EXTRA_LOCATION, waypoint);
        context.sendBroadcast(sendWaypointIntent);
    }

    private void handleSendWaypoint(Location waypoint) {
        Log.d(TAG, "handleSendWaypoint");

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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged");

        if (key.equals(getString(R.string.pref_key_cloud_storage_provider))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_cloud_storage_provider");

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String cloudProvider = sharedPref.getString(getString(R.string.pref_key_cloud_storage_provider), null);
            Log.d(TAG, "onSharedPreferenceChanged: cloudProvider=" + cloudProvider);

            handleSettingChangeCloud(cloudProvider);
        } else {
            Log.w(TAG, "onSharedPreferenceChanged: key=" + key);
        }
    }

    public void addSessionImageFileName(String imageFileName) {
        Log.d(TAG, "addSessionImageFileName");

        mImageFileNamesStack.push(imageFileName);
    }

    protected void handleCommandHhmdEmergency() {
        Log.d(TAG, "handleCommandHhmdEmergency");

        String sessionCloudLink = getSessionCloudLink();
        Location lastHhmdLocation = locationService.getLastHhmdLocation();
        emergencyService.startEmergency(lastHhmdLocation, sessionCloudLink);
        uasCommunicationService.startEmergency();
    }

    protected void handleCommandHhmdLight(boolean lightOnOff) {
        Log.d(TAG, "handleCommandHhmdLight: lightOnOff=" + lightOnOff);

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

        createCloudSessionFolder();

        locationService.startSession();
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd");

        locationService.stopSession();
    }

    private void handleCommandUasLocation(Location uasLocation) {
        Log.d(TAG, "handleCommandUasLocation: uasLocation=" + uasLocation);
        locationService.pushUasLocation(uasLocation);
    }

    // TODO: 5/19/2017 Make sure the image is saved to the cloud as well
    // TODO : Parameter(s) for incoming UAS image need(s) to be added.
    private void handleCommandUasImage() {
        Log.d(TAG, "handleCommandUasImage");

        String filename = getDateTime() + ".jpg";
        saveImage(filename);
        uploadImage(filename);
        addSessionImageFileName(filename);
        sendNewImageIntent();
    }

    private void handleSettingChangeCloud(String cloudType) {
        Log.d(TAG, "handleSettingChangeCloud");

        switch (cloudType) {
            case "0":
                Log.d(TAG, "handleSettingChangeCloud: Dropbox Specified");

                cloudStorage = new Dropbox(this, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
                createCloudAppFolder();
                break;
            case "1":
                Log.d(TAG, "handleSettingChangeCloud: Google Drive Specified");

//                cloudStorage = new GoogleDrive();
//                createCloudAppFolder();
                break;
            case "2":
                Log.d(TAG, "handleSettingChangeCloud: OneDrive Specified");

                cloudStorage = new OneDrive(this,ONE_DRIVE_APP_KEY,ONE_DRIVE_APP_SECRET);
                createCloudAppFolder();
                break;
            default:
                Log.d(TAG, "handleSettingChangeCloud: No Cloud Specified");
                //Possible set to device storage here
                //Note: Most of the cloud services allowed for saving in offline mode
                break;
        }
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

    private void createCloudAppFolder() {
        Log.d(TAG, "createCloudAppFolder");

        new Thread() {
            @Override
            public void run() {
                try {
                    cloudStorage.createFolder(CLOUD_APP_FOLDER);
                } catch (Exception e) {
                    Log.e(TAG, "createCloudAppFolder: Exception " + e.getMessage(), e);
                }
            }
        }.start();
    }

    private void createCloudSessionFolder() {
        Log.d(TAG, "createCloudSessionFolder");

        new Thread() {
            @Override
            public void run() {
                try {
                    cloudSessionFolder = CLOUD_APP_FOLDER + "/" + getDateTime();

                    cloudStorage.createFolder(cloudSessionFolder);
                } catch (Exception e) {
                    Log.e(TAG, "createCloudSessionFolder: Exception " + e.getMessage(), e);
                }
            }
        }.start();
    }

    private void uploadImage(final String filename) {
        Log.d(TAG, "uploadImage");

        new Thread() {
            @Override
            public void run() {
                try {
                    final InputStream inputStream = openFileInput(filename);
                    final String filePath = cloudSessionFolder + "/" + filename;
                    cloudStorage.upload(filePath, inputStream, 0, false);
                } catch (Exception e) {
                    Log.e(TAG, "uploadImage: Exception " + e.getMessage(), e);
                }
            }
        }.start();
    }

    private String getSessionCloudLink() {
        Log.d(TAG, "getSessionCloudLink");

        String link;
        if (cloudStorage != null) {
            link = cloudStorage.createShareLink(cloudSessionFolder);
        } else {
            Log.w(TAG, "getSessionCloudLink: No cloud storage");
            link = null;
        }

        return link;
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }

    private class CommandServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case SEND_WAYPOINT:
                        Location waypoint = intent.getExtras().getParcelable(EXTRA_LOCATION);
                        handleSendWaypoint(waypoint);
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
                    case SETTING_CHANGE_CLOUD:
                        String cloudType = intent.getStringExtra(EXTRA_CLOUD_TYPE);
                        handleSettingChangeCloud(cloudType);
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
