package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;

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
 */

public class CommandService extends Service {
    protected static final String ACTION_NEW_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_IMAGE";
    protected static final String ACTION_NEW_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_LOCATION";
    protected static final String REQUEST_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.REQUEST_UAS_IMAGE";
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
    protected static final String SETTING_EMERGENCY_ADD = "com.helpfromabove.helpfromabove.setting.SETTING_EMERGENCY_ADD";
    protected static final String SETTING_EMERGENCY_REMOVE = "com.helpfromabove.helpfromabove.setting.SETTING_EMERGENCY_REMOVE";
    protected static final String EXTRA_CLOUD_TYPE = "com.helpfromabove.helpfromabove.extra.EXTRA_CLOUD_TYPE";


    protected static final String CONSTANT_CLOUD_DROPBOX = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_DROPBOX";
    protected static final String CONSTANT_CLOUD_GOOGLEDRIVE = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_GOOGLEDRIVE";
    protected static final String CONSTANT_CLOUD_ONEDRIVE = "com.helpfromabove.helpfromabove.constant.CONSTANT_CLOUD_ONEDRIVE";

    //App keys needed for CloudRail and other Cloud Services
    private final static String CLOUDRAIL_LICENSE_KEY = "591cadfabac9e94ae79c9711";
    private final static String DROPBOX_APP_KEY = "5a95or0lhqau6y1";
    private final static String DROPBOX_APP_SECRET = "g31z4opqpzpklri";
    //Still need application keys for GoogleDrive and OneDrive
    private final static String GOOGLEDRIVE_APP_KEY = "";
    private final static String GOOGLEDRIVE_APP_SECRET = "";
    private final static String ONEDRIVE_APP_KEY = "";
    private final static String ONEDRIVE_APP_SECRET = "";

    private final static String TAG = "CommandService";
    private CommandServiceBroadcastReceiver cSBR = new CommandServiceBroadcastReceiver();
    private IntentFilter cSIF = new IntentFilter();
    private Stack<String> mImageFileNamesStack = new Stack<>();
    private CloudStorage cs;
//    private Stack<Location> mLocationStack = new Stack<>();

    private int imageDebugCounter = 0;
    private ArrayList<byte[]> imageBytes = new ArrayList<>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        CloudRail.setAppKey(CLOUDRAIL_LICENSE_KEY);
        mImageFileNamesStack = new Stack<>();
//        mLocationStack = new Stack<>();
//        TODO : add IntentFilter
        cSIF.addAction(REQUEST_UAS_IMAGE);
        cSIF.addAction(COMMAND_HHMD_EMERGENCY);
        cSIF.addAction(COMMAND_HHMD_LIGHT);
        cSIF.addAction(COMMAND_HHMD_UAS_HEIGHT_UP);
        cSIF.addAction(COMMAND_HHMD_UAS_HEIGHT_DOWN);
        cSIF.addAction(COMMAND_HHMD_SESSION_START);
        cSIF.addAction(COMMAND_HHMD_SESSION_END);
        cSIF.addAction(COMMAND_HHMD_LOCATION);
        cSIF.addAction(COMMAND_UAS_IMAGE);
        cSIF.addAction(COMMAND_UAS_LOCATION);
        cSIF.addAction(SETTING_CHANGE_CLOUD);
        cSIF.addAction(SETTING_CHANGE_START_HEIGHT);
        cSIF.addAction(SETTING_EMERGENCY_ADD);
        cSIF.addAction(SETTING_EMERGENCY_REMOVE);
        registerReceiver(cSBR, cSIF);

        Bitmap bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4240);
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baoStream);
        imageBytes.add(baoStream.toByteArray());
        bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4366);
        baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baoStream);
        imageBytes.add(baoStream.toByteArray());
        bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4491);
        baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baoStream);
        imageBytes.add(baoStream.toByteArray());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        unregisterReceiver(cSBR);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getLastSessionImageFileName() {
        String imageFileName;
        if (!mImageFileNamesStack.empty()) {
            imageFileName = mImageFileNamesStack.peek();
        } else {
            Log.d(TAG, "getLastSessionImageFileName: mImageFileNamesStack is empty.");
            imageFileName = null;
        }
        return imageFileName;
    }

    public void addSessionImageFileName(String imageFileName) {
        mImageFileNamesStack.push(imageFileName);
    }

    protected void handleCommandHhmdEmergency() {
        Log.d(TAG, "handleCommandHhmdEmergency: ");
    }

    protected void handleCommandHhmdLight(boolean lightOnOff) {
        Log.d(TAG, "handleCommandHhmdLight: lightOnOff=" + lightOnOff);
        if (imageDebugCounter >= 2) {
            Log.d(TAG, "handleCommandHhmdLight: incoming image example");
            handleCommandUasImage();
        }
        imageDebugCounter++;
    }

    protected void handleCommandHhmdUasHeightUp() {
        Log.d(TAG, "handleCommandHhmdUasHeightUp: ");
    }

    protected void handleCommandHhmdUasHeightDown() {
        Log.d(TAG, "handleCommandHhmdUasHeightDown: ");
    }

    protected void handleCommandHhmdSessionStart() {
        Log.d(TAG, "handleCommandHhmdSessionStart: ");
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd: ");
    }

    private void handleCommandHhmdLocation(Location location) {
        Log.d(TAG, "handleCommandHhmdLocation: location=" + location);
    }

    private void handleCommandUasLocation(Location location) {
        Log.d(TAG, "handleCommandUasLocation: location=" + location);
    }

    // TODO: 5/19/2017 Make sure the image is saved to the cloud aswell
    private void handleCommandUasImage() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZ");
        String filename = df.format(Calendar.getInstance().getTime()) + ".jpg";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(imageBytes.get(imageDebugCounter % 3));
            outputStream.close();
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }

        Log.d(TAG, "handleCommandUasImage: 5");

        addSessionImageFileName(filename);
        sendNewImageIntent();
    }
    /* This is being done inside of the CommandService because
     * passing around the CloudStorage Object doesn't make since.
     * It allows for one place to handle all operations to the
     * cloud such as uploading/deleting.
     */
    private void handleSettingChangeCloud(String cloudType) {

        switch (cloudType) {
            case CONSTANT_CLOUD_DROPBOX:
                cs = new Dropbox(this,DROPBOX_APP_KEY,DROPBOX_APP_SECRET);
                createFolderCloud(cs);
                Log.d(TAG, "handleSettingChangeCloud: Dropbox Specified");
                break;
            case CONSTANT_CLOUD_GOOGLEDRIVE:
                //cs = new GoogleDrive();
                // createFolderCloud(cs);
                Log.d(TAG, "handleSettingChangeCloud: Google Drive Specified");
                break;
            case CONSTANT_CLOUD_ONEDRIVE:
                //cs = new OneDrive(this,ONEDRIVE_APP_KEY,ONEDRIVE_APP_SECRET);
                //createFolderCloud(cs);
                Log.d(TAG, "handleSettingChangeCloud: OneDrive Specified");
                break;
            default:
                Log.d(TAG, "handleSettingChangeCloud: No Cloud Specified");
                //Possible set to device storage here
                //Note: Most of the cloud services allowed for saving in offline mode
        }
    }
    private void handleSettingChangeStartHeight() {
        Log.d(TAG, "handleSettingChangeStartHeight");
    }
    private void handleSettingEmergencyAdd() {
        Log.d(TAG, "handleSettingEmergencyAdd");
    }
    private void handleSettingemergencyRemove() {
        Log.d(TAG, "handleSettingemergencyRemove");
    }

    private void sendNewImageIntent() {
        Log.d(TAG, "sendNewImageIntent: ");
        Intent newImageIntent = new Intent(ACTION_NEW_UAS_IMAGE);
        newImageIntent.putExtra(EXTRA_IMAGE_FILE_NAME, getLastSessionImageFileName());
        sendBroadcast(newImageIntent);
    }

    private void createFolderCloud (final CloudStorage cs) {
        new Thread() {
            @Override
            public void run() {
                try {
                    cs.createFolder("/TestFolder");
                } catch (Exception e) {
                    Log.d(TAG, "Error: "+ e.getMessage());
                }
            }
        }.start();
    }

    private class CommandServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case REQUEST_UAS_IMAGE:
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
                    case SETTING_EMERGENCY_ADD:
                        handleSettingEmergencyAdd();
                        break;
                    case SETTING_EMERGENCY_REMOVE:
                        handleSettingemergencyRemove();
                        break;
                    default:
                        Log.d(TAG, "onReceive: default: action=" + action);
                }
            }
        }
    }
}
