package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    protected static final String EXTRA_LIGHT_ON_OFF = "com.helpfromabove.helpfromabove.extra.EXTRA_LIGHT_ON_OFF";
    protected static final String EXTRA_IMAGE_FILE_NAME = "com.helpfromabove.helpfromabove.extra.EXTRA_IMAGE_FILE_NAME";
    protected static final String EXTRA_LOCATION = "com.helpfromabove.helpfromabove.extra.EXTRA_LOCATION";
    protected static final String COMMAND_HHMD_EMERGENCY = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_EMERGENCY";
    protected static final String COMMAND_HHMD_LIGHT = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_LIGHT";
    protected static final String COMMAND_HHMD_UAS_HEIGHT_UP = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_UAS_HEIGHT_UP";
    protected static final String COMMAND_HHMD_UAS_HEIGHT_DOWN = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_UAS_HEIGHT_DOWN";
    protected static final String COMMAND_HHMD_SESSION_START = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_SESSION_START";
    protected static final String COMMAND_HHMD_SESSION_END = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_SESSION_END";
    protected static final String COMMAND_HHMD_LOCATION = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_LOCATION";
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

    //App keys needed for CloudRail and other Cloud Services
    private final static String CLOUDRAIL_LICENSE_KEY = "591cadfabac9e94ae79c9711";
    private final static String DROPBOX_APP_KEY = "5a95or0lhqau6y1";
    private final static String DROPBOX_APP_SECRET = "g31z4opqpzpklri";
    //Still need application keys for GoogleDrive and OneDrive
    private final static String GOOGLE_DRIVE_APP_KEY = "";
    private final static String GOOGLE_DRIVE_APP_SECRET = "";
    private final static String ONE_DRIVE_APP_KEY = "";
    private final static String ONE_DRIVE_APP_SECRET = "";

    private final static String TAG = "CommandService";
    private CommandServiceBroadcastReceiver commandServiceBroadcastReceiver = new CommandServiceBroadcastReceiver();
    private IntentFilter intentFilter = new IntentFilter();
    private Stack<String> mImageFileNamesStack = new Stack<>();
    private CloudStorage cloudStorage;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private WifiP2pManager.ActionListener wifiP2pListener = new WifiP2pManager.ActionListener() {
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
        intentFilter.addAction(COMMAND_HHMD_EMERGENCY);
        intentFilter.addAction(COMMAND_HHMD_LIGHT);
        intentFilter.addAction(COMMAND_HHMD_UAS_HEIGHT_UP);
        intentFilter.addAction(COMMAND_HHMD_UAS_HEIGHT_DOWN);
        intentFilter.addAction(COMMAND_HHMD_SESSION_START);
        intentFilter.addAction(COMMAND_HHMD_SESSION_END);
        intentFilter.addAction(COMMAND_HHMD_LOCATION);
        intentFilter.addAction(COMMAND_UAS_IMAGE);
        intentFilter.addAction(COMMAND_UAS_LOCATION);
        intentFilter.addAction(SETTING_CHANGE_CLOUD);
        intentFilter.addAction(SETTING_CHANGE_START_HEIGHT);
        intentFilter.addAction(SETTING_EMERGENCY_CONTACT_ADD);
        intentFilter.addAction(SETTING_EMERGENCY_CONTACT_REMOVE);
        registerReceiver(commandServiceBroadcastReceiver, intentFilter);

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
    }

    protected void handleCommandHhmdUasHeightDown() {
        Log.d(TAG, "handleCommandHhmdUasHeightDown");
    }

    protected void handleCommandHhmdSessionStart() {
        Log.d(TAG, "handleCommandHhmdSessionStart");
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd");
    }

    private void handleCommandHhmdLocation(Location location) {
        Log.d(TAG, "handleCommandHhmdLocation: location=" + location);
    }

    private void handleCommandUasLocation(Location location) {
        Log.d(TAG, "handleCommandUasLocation: location=" + location);
    }

    // TODO: 5/19/2017 Make sure the image is saved to the cloud as well
    // TODO : Parameter(s) for incoming UAS image need(s) to be added.
    private void handleCommandUasImage() {
        Log.d(TAG, "handleCommandUasImage");

        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZ");
        String filename = df.format(Calendar.getInstance().getTime()) + ".jpg";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);

            // This is for local image testing. Modify once local image testing is complete
            outputStream.write(imageBytes.get(imageDebugCounter % 3));

            outputStream.close();
        } catch (IOException iOE) {
            Log.e(TAG, "handleCommandUasImage: IOException: " + iOE.getMessage(), iOE);
        }

        addSessionImageFileName(filename);
        sendNewImageIntent();
    }

    /*
     * This is being done inside of the CommandService because
     * passing around the CloudStorage Object doesn't make sense.
     * It allows for one place to handle all operations to the
     * cloud such as uploading/deleting.
     */
    private void handleSettingChangeCloud(String cloudType) {
        Log.d(TAG, "handleSettingChangeCloud");

        switch (cloudType) {
            case "0":
                Log.d(TAG, "handleSettingChangeCloud: Dropbox Specified");

                cloudStorage = new Dropbox(this, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
                createFolderCloud(cloudStorage);
                break;
            case "1":
                Log.d(TAG, "handleSettingChangeCloud: Google Drive Specified");

                //cloudStorage = new GoogleDrive();
                // createFolderCloud(cloudStorage);
                break;
            case "2":
                Log.d(TAG, "handleSettingChangeCloud: OneDrive Specified");

                //cloudStorage = new OneDrive(this,ONEDRIVE_APP_KEY,ONEDRIVE_APP_SECRET);
                //createFolderCloud(cloudStorage);
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


    private void createFolderCloud(final CloudStorage cs) {
        Log.d(TAG, "createFolderCloud");

        new Thread() {
            @Override
            public void run() {
                try {
                    cs.createFolder("/TestFolder");
                } catch (Exception e) {
                    Log.e(TAG, "createFolderCloud: Exception " + e.getMessage(), e);
                }
            }
        }.start();
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
                    case COMMAND_HHMD_LOCATION:
                        final Location hhmdLocation = intent.getExtras().getParcelable(EXTRA_LOCATION);
                        handleCommandHhmdLocation(hhmdLocation);
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
}
