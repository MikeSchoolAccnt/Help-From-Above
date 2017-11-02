package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.OneDrive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static String LOCAL_APP_FOLDER;
    private static final String CLOUD_APP_FOLDER = "/" + APP_FOLDER;
    private String sessionFolder;
    private CompressFormat compressionFormat;
    private int compressionQuality;

    //Read that this is thread safe and using it to check that all images have been uploaded.
    private AtomicInteger atomicImageUploadCount = new AtomicInteger(0);

    private CloudStorage cloudStorage;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        LOCAL_APP_FOLDER = getFilesDir() + "/" + APP_FOLDER;

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

        File directory = new File(LOCAL_APP_FOLDER);
        if (!directory.mkdirs()) {
            Log.e(TAG, "Could not make directory: " + LOCAL_APP_FOLDER);
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
        Log.d(TAG, "startSession: NOT YET IMPLEMENTED!");
    }

    protected void onLocationCalibrationComplete() {
        Log.d(TAG, "onLocationCalibrationComplete");

        atomicImageUploadCount = new AtomicInteger(0);
        createSessionFolder();
        compressionFormat = CompressFormat.JPEG;
        compressionQuality = 50;
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

        sessionFolder = CLOUD_APP_FOLDER + "/" + getDateTime();

        try {
            cloudStorage.createFolder(sessionFolder);
        } catch (com.cloudrail.si.exceptions.HttpException ex) {
            Log.e(TAG,ex.getMessage());
        }
    }

    private void createLocalSessionFolder() {
        Log.d(TAG, "createLocalSessionFolder");

        sessionFolder = LOCAL_APP_FOLDER + "/" + getDateTime();
        File directory = new File(sessionFolder);
        if (!directory.mkdirs()) {
            Log.e(TAG, "Could not make directories: " + sessionFolder);
        }
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }

    private String getImageFileExtension() {
        String extension;
        if (compressionFormat == CompressFormat.PNG) {
            extension = ".png";
        }
        else if (compressionFormat == CompressFormat.JPEG) {
            extension = ".jpg";
        }
        else {
            Log.w(TAG, "Unknown compression format: " + compressionFormat);
            extension = null;
        }

        return extension;
    }

    protected int getUploadCount(){
        return atomicImageUploadCount.get();
    }

    protected void saveImage(final Bitmap bitmap) {
        Log.d(TAG, "saveImage");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String filename = getDateTime() + getImageFileExtension();
                String path = sessionFolder + "/" + filename;

                if (cloudStorage == null) {
                    saveLocalImage(bitmap, path);
                } else {
                    saveCloudImage(bitmap, path);
                }
            }
        }).start();
    }

    private void saveLocalImage(Bitmap bitmap, final String path) {
        Log.d(TAG, "saveLocalImage");

        try {
            byte[] byteArray = convertBitmapToByteArray(bitmap, compressionFormat, compressionQuality);

            File file = new File(path);
            file.createNewFile();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(byteArray);
            fos.flush();
            fos.close();
        } catch (IOException iOE) {
            Log.e(TAG, "saveLocalImage: IOException: " + iOE.getMessage(), iOE);
        }
    }

    private static byte[] convertBitmapToByteArray(Bitmap bitmap, CompressFormat format, int quality) {
        byte[] byteArray;
        if (bitmap == null) {
            byteArray = null;
        }
        else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(format, quality, bos);
            byteArray = bos.toByteArray();
        }

        return byteArray;
    }

    private void saveCloudImage(Bitmap bitmap, final String path) {
        Log.d(TAG, "saveCloudImage");

        ByteArrayInputStream byteArrayInputStream = convertDataToByteArrayInputStream(bitmap, compressionFormat, compressionQuality);

        cloudStorage.upload(path, byteArrayInputStream, byteArrayInputStream.available(), false);

        //Same as <int>++
        atomicImageUploadCount.getAndIncrement();
    }

    private static ByteArrayInputStream convertDataToByteArrayInputStream(Bitmap bitmap, CompressFormat format, int quality) {
        Log.d(TAG, "convertDataToBitmap");

        ByteArrayInputStream byteArrayInputStream;

        if(bitmap == null || bitmap.getByteCount() == 0) {
            byteArrayInputStream = null;
        }
        else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(format, quality, byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        }

        return byteArrayInputStream;
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
