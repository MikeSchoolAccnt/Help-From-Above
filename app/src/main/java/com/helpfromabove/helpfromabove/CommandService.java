package com.helpfromabove.helpfromabove;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
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
    protected static final String ACTION_NEW_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_IMAGE";
    protected static final String ACTION_NEW_UAS_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_LOCATION";
    protected static final String ACTION_NEW_HHMD_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_HHMD_LOCATION";
    protected static final String ACTION_REQUEST_LAST_IMAGE_FILENAME = "com.helpfromabove.helpfromabove.action.ACTION_REQUEST_LAST_IMAGE_FILENAME";
    protected static final String ACTION_CONNECT_WIFI_P2P = "com.helpfromabove.helpfromabove.action.ACTION_CONNECT_WIFI_P2P";
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
    protected static final String SETTING_EMERGENCY_CONTACT_ADD = "com.helpfromabove.helpfromabove.setting.SETTING_EMERGENCY_CONTACT_ADD";
    protected static final String SETTING_EMERGENCY_CONTACT_REMOVE = "com.helpfromabove.helpfromabove.setting.SETTING_EMERGENCY_CONTACT_REMOVE";
    protected static final String EXTRA_CLOUD_TYPE = "com.helpfromabove.helpfromabove.extra.EXTRA_CLOUD_TYPE";
    protected static final String CONSTANT_CLOUD_DROPBOX = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_DROPBOX";
    protected static final String CONSTANT_CLOUD_GOOGLE_DRIVE = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_GOOGLE_DRIVE";
    protected static final String CONSTANT_CLOUD_ONE_DRIVE = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_ONE_DRIVE";
    protected static final int CONSTANT_LOCATION_UPDATE_SECONDS = 3;

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
    private final String CLOUD_APP_FOLDER = "/" + "Help_From_Above";
    private String cloudSessionFolder;
    private CommandServiceBroadcastReceiver commandServiceBroadcastReceiver = new CommandServiceBroadcastReceiver();
    private IntentFilter intentFilter = new IntentFilter();
    private Stack<String> mImageFileNamesStack = new Stack<>();
    private CloudStorage cloudStorage;
    private LocationManager locationManager;
    private LocationListener commandLocationListener;
    private Criteria locationCriteria = new Criteria();
    private Stack<Location> hhmdLocations = new Stack<>();
    private Stack<Location> uasLocations = new Stack<>();
    private int heightOffset;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private WifiP2pManager.ActionListener wifiP2pListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Log.d(TAG, "WifiP2pManager.ActionListener: onSuccess");
        }

        @Override
        public void onFailure(int reasonCode) {
            Log.d(TAG, "WifiP2pManager.ActionListener: onFailure: reasonCode=" + reasonCode);
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
    };

    // This is for local image testing. Remove once local image testing is complete
    private int imageDebugCounter = 0;
    private ArrayList<byte[]> imageBytes = new ArrayList<>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        CloudRail.setAppKey(CLOUDRAIL_LICENSE_KEY);

        mImageFileNamesStack = new Stack<>();

        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        wifiP2pManager.discoverPeers(wifiP2pChannel, wifiP2pListener);

        intentFilter.addAction(ACTION_REQUEST_LAST_IMAGE_FILENAME);
        intentFilter.addAction(ACTION_CONNECT_WIFI_P2P);
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
        intentFilter.addAction(SETTING_EMERGENCY_CONTACT_ADD);
        intentFilter.addAction(SETTING_EMERGENCY_CONTACT_REMOVE);
        registerReceiver(commandServiceBroadcastReceiver, intentFilter);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

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

        unregisterReceiver(commandServiceBroadcastReceiver);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

    private void pushUasLocation(Location uasLocation) {
        Log.d(TAG, "pushUasLocation: uasLocation=" + uasLocation);
        uasLocations.push(uasLocation);
    }

    private Location getLastUasLocation() {
        Log.d(TAG, "getLastUasLocation");

        Location uasLocation = null;
        if (!uasLocations.isEmpty()) {
            uasLocation = uasLocations.peek();
        } else {
            Log.w(TAG, "getLastUasLocation: uasLocations is empty.");
        }

        return uasLocation;
    }

    private void pushHhmdLocation(Location hhmdLocation) {
        Log.d(TAG, "pushHhmdLocation: hhmdLocation=" + hhmdLocation);
        hhmdLocations.push(hhmdLocation);
    }

    private Location getLastHhmdLocation() {
        Log.d(TAG, "getLastHhmdLocation");

        Location hhmdLocation = null;
        if (!hhmdLocations.isEmpty()) {
            hhmdLocation = hhmdLocations.peek();
        } else {
            Log.w(TAG, "getLastUasLocation: hhmdLocations is empty.");
        }

        return hhmdLocation;
    }

    private Location getPreviousHhmdLocation() {
        Log.d(TAG, "getPreviousHhmdLocation");

        Location previousHhmdLocation = null;
        if (hhmdLocations.size() >= 2) {
            Location lastHhmdLocation = hhmdLocations.pop();
            previousHhmdLocation = hhmdLocations.peek();
            hhmdLocations.push(lastHhmdLocation);
        } else {
            Log.w(TAG, "getPreviousHhmdLocation: hhmdLocations does not have enough locations.");
        }

        return previousHhmdLocation;
    }

    private synchronized int getHeightOffset() {
        Log.d(TAG, "getHeightOffset");

        return heightOffset;
    }

    private synchronized void setHeightOffset(int i) {
        Log.d(TAG, "setHeightOffset: i=" + i);

        heightOffset = i;
    }

    private synchronized void clearHeightOffset() {
        Log.d(TAG, "clearHeightOffset");

        setHeightOffset(0);
    }

    private void resetHeightOffset() {
        Log.d(TAG, "resetHeightOffset");

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int startHeight = Integer.parseInt(sharedPref.getString(getString(R.string.pref_key_uas_start_height), getString(R.string.pref_value_uas_start_height_default)));
        setHeightOffset(startHeight);
    }

    private void incrementHeightOffset() {
        Log.d(TAG, "incrementHeightOffset");

        setHeightOffset(heightOffset++);
    }

    private void decrementHeightOffset() {
        Log.d(TAG, "decrementHeightOffset");

        setHeightOffset(heightOffset--);
    }

    private Location getLocationDiff(Location newLocation, Location oldLocation) {
        Log.d(TAG, "getLocationDiff");

        Location diff = null;
        if (newLocation != null && oldLocation != null) {
            diff = new Location(getClass().getName());

            double diffLong = newLocation.getLongitude() - oldLocation.getLongitude();
            double diffLat = newLocation.getLatitude() - oldLocation.getLatitude();
            double diffAlt = newLocation.getAltitude() - oldLocation.getAltitude();
            float diffAcc = newLocation.getAccuracy() + newLocation.getAccuracy();

            diff.setLongitude(diffLong);
            diff.setLatitude(diffLat);
            diff.setAltitude(diffAlt);
            diff.setAccuracy(diffAcc);
        } else {
            Log.e(TAG, "getLocationSum: Locations cannot be subtracted because a location object is null.");
        }

        return diff;
    }

    private Location addLocations(Location location1, Location location2) {
        Log.d(TAG, "addLocations");

        Location sum = null;
        if (location1 != null && location2 != null) {
            sum = new Location(getClass().getName());
            double retLong = location1.getLongitude() + location2.getLongitude();
            double retLat = location1.getLatitude() + location2.getLatitude();
            double retAlt = location1.getAltitude() + location2.getAltitude();
            float retAcc = location1.getAccuracy() + location2.getAccuracy();

            sum.setLongitude(retLong);
            sum.setLatitude(retLat);
            sum.setAltitude(retAlt);
            sum.setAccuracy(retAcc);

        } else {
            Log.e(TAG, "addLocations: Locations cannot be added because a location object is null.");
        }

        return sum;
    }

    private Location addHeightOffset(Location location) {
        Log.d(TAG, "addHeightOffset");

        location.setAltitude(location.getAltitude() + getHeightOffset());
        clearHeightOffset();
        return location;
    }

    private Location generateWaypoint() {
        Log.d(TAG, "generateWaypoint");

        Location lastHhmd = getLastHhmdLocation();
        Location previousHhmd = getPreviousHhmdLocation();
        Location lastUas = getLastUasLocation();
        Location diff = getLocationDiff(lastHhmd, previousHhmd);
        Location waypoint = addLocations(lastUas, diff);

        return addHeightOffset(waypoint);
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
        } else if (key.equals(getString(R.string.pref_key_emergency_message_name))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_emergency_message_name");
        } else if (key.equals(getString(R.string.pref_key_emergency_message_text))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_emergency_message_text");
        } else if (key.equals(getString(R.string.pref_key_emergency_contacts))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_emergency_contacts");
        } else {
            Log.w(TAG, "onSharedPreferenceChanged: key=" + key);
        }
    }

    public void addSessionImageFileName(String imageFileName) {
        Log.d(TAG, "addSessionImageFileName");

        mImageFileNamesStack.push(imageFileName);
    }

    private void connectToDevice(WifiP2pDevice device) {
        Log.d(TAG, "connectToDevice: device.toString()=" + device.toString());

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(wifiP2pChannel, config, wifiP2pListener);

        startActivity(new Intent(getApplicationContext(),MainActivity.class));
    }

    protected void handleCommandHhmdEmergency() {

        Log.d(TAG, "handleCommandHhmdEmergency");

        PreferenceScreen screen = SettingsActivity.emergencyContactsPreferenceScreen;

        if(screen == null){
            Log.d(TAG,"Implement a way to grab contacts is not already done.");
        }
        //Lists all contacts that are checked as active. Can be used later for making messages.
        else {
            for (int i = 0; i < screen.getPreferenceCount(); i++){
                CheckBoxPreference box = (CheckBoxPreference) screen.getPreference(i);

                if(box.isChecked()) {
                    Log.d(TAG, box.getTitle() + " : " + box.getSummary());
                }
            }
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> set = sharedPref.getStringSet(getString(R.string.pref_key_emergency_contacts), new HashSet<String>());
        for (String id : set) {
            Log.d(TAG, "emergencyContactId=" + id);
        }

        String userName = sharedPref.getString(getString(R.string.pref_key_emergency_message_name),getString(R.string.pref_default_emergency_message_name));
        String emergencyMessage = sharedPref.getString(getString(R.string.pref_key_emergency_message_text),"Should no happen");

        //TODO: replace the default GPS string and CloudLink String in third emergency_message_text
        emergencyMessage = emergencyMessage.replace("(Name)",userName);

        //Input your own information here for testing
        //sendSMSMessage("Your test number",emergencyMessage);
    }

    //Allows for sending a message to someone through the application
    //The message passed in this might not be needed instead it can be grabbed from shared preferences.
    //TODO: Figure a way to request permisions since this is not an activity.
    private void sendSMSMessage(String recipientNumber, String message){

//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            int response = getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
//              if (response == PackageManager.PERMISSION_GRANTED) {

                //This try catch is only for if an invalid number or message is trying to be sent.
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(recipientNumber, null, message, null, null);
                } catch (IllegalArgumentException e){
                    Log.e(TAG,"sendSMSMessage:" + e.getMessage());
                }


//            } else {
//                  ActivityCompat.requestPermissions("Need Activity Here",new String[]{android.Manifest.permission.SEND_SMS},0);
//            }
//        }


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

        incrementHeightOffset();
    }

    protected void handleCommandHhmdUasHeightDown() {
        Log.d(TAG, "handleCommandHhmdUasHeightDown");

        decrementHeightOffset();
    }

    protected void handleCommandHhmdSessionStart() {
        Log.d(TAG, "handleCommandHhmdSessionStart");

        createCloudSessionFolder();

        requestLocationUpdates();
        resetHeightOffset();
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd");

        stopLocationUpdates();
    }

    private void handleCommandHhmdLocation(Location hhmdLocation) {
        Log.d(TAG, "handleCommandHhmdLocation: location=" + hhmdLocation);
        pushHhmdLocation(hhmdLocation);

        Location waypoint = generateWaypoint();
    }

    private void handleCommandUasLocation(Location uasLocation) {
        Log.d(TAG, "handleCommandUasLocation: uasLocation=" + uasLocation);
        pushUasLocation(uasLocation);
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

    private void handleSettingEmergencyContactAdd() {
        Log.d(TAG, "handleSettingEmergencyContactAdd");
    }

    private void handleSettingEmergencyContactRemove() {
        Log.d(TAG, "handleSettingEmergencyContactRemove");
    }

    private void sendNewImageIntent() {
        Log.d(TAG, "sendNewImageIntent");

        Intent newImageIntent = new Intent(ACTION_NEW_UAS_IMAGE);
        newImageIntent.putExtra(EXTRA_IMAGE_FILE_NAME, getLastSessionImageFileName());
        sendBroadcast(newImageIntent);
    }

    private void requestLocationUpdates() {
        Log.d(TAG, "requestLocationUpdates");

        commandLocationListener = new CommandLocationListener();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int response = getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (response == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: permission FEATURE_LOCATION is GRANTED");
                locationManager.requestLocationUpdates(1000 * CONSTANT_LOCATION_UPDATE_SECONDS, 0, locationCriteria, commandLocationListener, getMainLooper());
            } else {
                Log.w(TAG, "onCreate: permission FEATURE_LOCATION is DENIED");
                // TODO : Request permission from user on devices at or above Android M
                // Because the user can explicitly modify the permissions of apps, if the
                // user denies Location from this app, then a dialog box should be shown
                // to the user to give this access to Location. This should send a
                // broadcast that gets received from all activities. When an activity
                // gets the broadcast, it should then request the permission.
            }
        } else {
            locationManager.requestLocationUpdates(1000 * CONSTANT_LOCATION_UPDATE_SECONDS, 0, locationCriteria, commandLocationListener, getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates");

        locationManager.removeUpdates(commandLocationListener);
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
                    case ACTION_REQUEST_LAST_IMAGE_FILENAME:
                        sendNewImageIntent();
                        break;
                    case ACTION_CONNECT_WIFI_P2P:
                        WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                        connectToDevice(device);
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
                    case SETTING_EMERGENCY_CONTACT_ADD:
                        handleSettingEmergencyContactAdd();
                        break;
                    case SETTING_EMERGENCY_CONTACT_REMOVE:
                        handleSettingEmergencyContactRemove();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }

    private class CommandLocationListener implements LocationListener {
        private static final String TAG = "CommandLocationListener";

        CommandLocationListener() {
            Log.d(TAG, "CommandLocationListener");
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged");
            handleCommandHhmdLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: status=" + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: provider=" + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: provider=" + provider);
        }
    }
}
