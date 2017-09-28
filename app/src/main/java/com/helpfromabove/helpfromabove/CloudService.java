package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.OneDrive;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CloudService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "CloudService";

    private final static String CLOUDRAIL_LICENSE_KEY = "59c031993d7042599787c8a8";
    private final static String DROPBOX_APP_KEY = "th6i7dbzxmnzbu5";
    private final static String DROPBOX_APP_SECRET = "22vq1tpd68tm28l";
    private final static String GOOGLE_DRIVE_APP_KEY = "";
    private final static String GOOGLE_DRIVE_APP_SECRET = "";
    private final static String ONE_DRIVE_APP_KEY = "8c273966-c62c-48b0-8500-d42b849bbf18";
    private final static String ONE_DRIVE_APP_SECRET = "3tGij2AmL0dGx7pHkukgK9o";

    private final IBinder mBinder = new CloudServiceBinder();

    private final String CLOUD_APP_FOLDER = "/" + "Help_From_Above";
    private String cloudSessionFolder;
    private CloudStorage cloudStorage;

    public CloudService() {
        super();

        Log.d(TAG, "CloudService");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        CloudRail.setAppKey(CLOUDRAIL_LICENSE_KEY);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");

        return mBinder;
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
        }
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

    private void createCloudAppFolder() {
        Log.d(TAG, "createCloudAppFolder");

       cloudStorage.createFolder(CLOUD_APP_FOLDER);
    }

    protected void startSession() {
        Log.d(TAG, "startSession");

        createCloudSessionFolder();
    }

    private void createCloudSessionFolder() {
        Log.d(TAG, "createCloudSessionFolder");

        cloudSessionFolder = CLOUD_APP_FOLDER + "/" + getDateTime();
        cloudStorage.createFolder(cloudSessionFolder);
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }

    private void uploadImage(final String filename) {
        Log.d(TAG, "uploadImage");

        try {
            final InputStream inputStream = openFileInput(filename);
            final String filePath = cloudSessionFolder + "/" + filename;
            cloudStorage.upload(filePath, inputStream, 0, false);
        } catch (IOException iOE) {
            Log.e(TAG, "uploadImage: Exception " + iOE.getMessage(), iOE);
        }
    }

    protected String getSessionCloudLink() {
        Log.d(TAG, "getSessionCloudLink");

        String link = null;
        if (cloudStorage != null) {
            link = cloudStorage.createShareLink(cloudSessionFolder);
        } else {
            Log.w(TAG, "getSessionCloudLink: No cloud storage");
        }

        return link;
    }



    protected class CloudServiceBinder extends Binder {
        private static final String TAG = "CloudServiceBinder";

        protected CloudService getService() {
            Log.d(TAG, "getService");

            return CloudService.this;
        }
    }

}
