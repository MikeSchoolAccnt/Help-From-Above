package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.OneDrive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

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

    private static final String APP_FOLDER = "Help_From_Above";
    private final String CLOUD_APP_FOLDER = "/" + APP_FOLDER;
    private String sessionFolder;

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

        initCloudStorage();
        createAppFolder();
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

            initCloudStorage();
            createAppFolder();
        }
    }

    private void initCloudStorage() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String cloudType = sharedPref.getString(getString(R.string.pref_key_cloud_storage_provider), getString(R.string.pref_value_cloud_storage_provider_default));

        switch (cloudType) {
            case "-1":
                Log.d(TAG, "initCloudStorage: None specified");
                cloudStorage = null;
                break;
            case "0":
                Log.d(TAG, "initCloudStorage: Dropbox specified");
                cloudStorage = new Dropbox(this, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
                break;
            case "1":
                Log.d(TAG, "initCloudStorage: Google Drive specified");
//                cloudStorage = new GoogleDrive();
//                createCloudAppFolder();
                break;
            case "2":
                Log.d(TAG, "initCloudStorage: OneDrive specified");
                cloudStorage = new OneDrive(this, ONE_DRIVE_APP_KEY, ONE_DRIVE_APP_SECRET);
                break;
            default:
                Log.w(TAG, "initCloudStorage: Unknown cloud storage specified");
                break;
        }
    }

    private void createAppFolder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (cloudStorage == null) {
                    createLocalAppFolder();
                } else {
                    createCloudAppFolder();
                }
            }
        }).start();
    }

    private void createLocalAppFolder() {
        Log.d(TAG, "createLocalAppFolder");

        String dir = Environment.getExternalStorageDirectory() + File.separator + CLOUD_APP_FOLDER;
        File directory = new File(dir);
        if (!directory.mkdirs()) {
            Log.e(TAG, "Could not make directories: " + dir);
        }
    }

    private void createCloudAppFolder() {
        Log.d(TAG, "createCloudAppFolder");

        try{
            cloudStorage.createFolder(CLOUD_APP_FOLDER);
        } catch (com.cloudrail.si.exceptions.HttpException hE){
            Log.e(TAG, hE.getMessage());
        }
    }

    protected void startSession() {
        Log.d(TAG, "startSession");

        createSessionFolder();
    }

    private void createSessionFolder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (cloudStorage == null) {
                    createLocalSessionFolder();
                } else {
                    createCloudSessionFolder();
                }
            }
        }).start();
    }

    private void createCloudSessionFolder() {
        Log.d(TAG, "createCloudSessionFolder");

        String cloudSessionFolder = CLOUD_APP_FOLDER + "/" + getDateTime();

        try {
            cloudStorage.createFolder(cloudSessionFolder);
        } catch (com.cloudrail.si.exceptions.HttpException ex) {
            Log.e(TAG,ex.getMessage());
        }
    }

    private void createLocalSessionFolder() {
        Log.d(TAG, "createLocalSessionFolder");

        sessionFolder = Environment.getExternalStorageDirectory() + File.separator + APP_FOLDER + getDateTime();
        File directory = new File(sessionFolder);
        if (!directory.mkdirs()) {
            Log.e(TAG, "Could not make directories: " + sessionFolder);
        }
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }

    protected void saveImage(final Object data) {
        Log.d(TAG, "saveImage");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (cloudStorage == null) {
                    saveLocalImage(data);
                } else {
                    saveCloudImage(data);
                }
            }
        }).start();
    }

    private void saveLocalImage(final Object data) {
        Log.d(TAG, "saveLocalImage");

//        // TODO: Determine filename either from data, or from getDateTime();
//        String filename = null;
//        String path = sessionFolder + File.separator + filename;
//        try {
//            File file = new File(path);
//            file.createNewFile();
//        } catch (IOException iOE) {
//            Log.e(TAG, "saveLocalImage: IOException: " + iOE.getMessage(), iOE);
//        }
//
//        try {
//            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().openFileOutput(path, Context.MODE_PRIVATE));
//        // TODO: Determine what data type will be and how to convert it so we can write it
//            outputStreamWriter.write(data);
//            outputStreamWriter.close();
//        } catch (IOException iOE) {
//            Log.e(TAG, "saveLocalImage: IOException: " + iOE.getMessage(), iOE);
//        }
    }

    private void saveCloudImage(final Object data) {
        Log.d(TAG, "saveCloudImage");

//        // TODO: Determine filename either from data, or from getDateTime();
//        String filename = null;
//        // TODO: Determine what data type will be and how to convert it to input steam
//        InputStream inputStream = null;
//        cloudStorage.upload(filename, inputStream, 0, false);
    }

    protected String getSessionCloudLink() {
        Log.d(TAG, "getSessionCloudLink");

        String link = null;
        if (cloudStorage != null) {
            link = cloudStorage.createShareLink(sessionFolder);
        } else {
            Log.d(TAG, "getSessionCloudLink: No cloud storage");
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
