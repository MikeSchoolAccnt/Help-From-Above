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
import com.cloudrail.si.exceptions.AuthenticationException;
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

public class CloudService extends Service {
    private static final String TAG = "CloudService";

    private final static String CLOUDRAIL_LICENSE_KEY = "59c031993d7042599787c8a8";
    private final static String DROPBOX_APP_KEY = "th6i7dbzxmnzbu5";
    private final static String DROPBOX_APP_SECRET = "22vq1tpd68tm28l";
    private final static String GOOGLE_DRIVE_APP_KEY = "";
    private final static String GOOGLE_DRIVE_APP_SECRET = "";
    private final static String ONE_DRIVE_APP_KEY = "8c273966-c62c-48b0-8500-d42b849bbf18";
    private final static String ONE_DRIVE_APP_SECRET = "3tGij2AmL0dGx7pHkukgK9o";
    private static final String APP_FOLDER = "Help_From_Above";
    private static final String LOCAL_APP_FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + APP_FOLDER;
    private static final String CLOUD_APP_FOLDER = "/" + APP_FOLDER;
    private final IBinder mBinder = new CloudServiceBinder();
    private String sessionFolder;
    private CompressFormat compressionFormat;
    private int compressionQuality;

    //Read that this is thread safe and using it to check that all images have been uploaded.
    private AtomicInteger atomicImageUploadCount = new AtomicInteger(0);

    private CloudStorage cloudStorage;

    private static byte[] convertBitmapToByteArray(Bitmap bitmap, CompressFormat format, int quality) {
        byte[] byteArray;
        if (bitmap == null) {
            byteArray = null;
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(format, quality, bos);
            byteArray = bos.toByteArray();
        }

        return byteArray;
    }

    private static ByteArrayInputStream convertDataToByteArrayInputStream(Bitmap bitmap, CompressFormat format, int quality) {
        ByteArrayInputStream byteArrayInputStream;

        if (bitmap == null || bitmap.getByteCount() == 0) {
            byteArrayInputStream = null;
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(format, quality, byteArrayOutputStream);

            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        }

        return byteArrayInputStream;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CloudRail.setAppKey(CLOUDRAIL_LICENSE_KEY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void initCloudStorage() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String cloudType = sharedPref.getString(getString(R.string.pref_key_cloud_storage_provider), getString(R.string.pref_value_cloud_storage_provider_default));

        switch (cloudType) {
            case "-1":
                cloudStorage = null;
                break;
            case "0":
                cloudStorage = new Dropbox(this, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
                break;
            case "1":
//                cloudStorage = new GoogleDrive();
//                createCloudAppFolder();
                break;
            case "2":
                cloudStorage = new OneDrive(this, ONE_DRIVE_APP_KEY, ONE_DRIVE_APP_SECRET);
                break;
            default:
                Log.w(TAG, "initCloudStorage: Unknown cloud storage specified");
                break;
        }

    }

    private void cloudStorageLogin() {
        if (cloudStorage != null) {
            try {
                cloudStorage.login();
            } catch (AuthenticationException aE) {
                Log.e(TAG, aE.getLocalizedMessage());
                Log.i(TAG, "cloudStorageLogin: Reinitializing cloudStorage to use local image storage.");

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                sharedPref.edit()
                        .putString(getString(R.string.pref_key_cloud_storage_provider), getString(R.string.pref_value_cloud_storage_provider_local))
                        .apply();
                initCloudStorage();
            }
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
        File directory = new File(LOCAL_APP_FOLDER);
        if (!directory.mkdirs()) {
            Log.e(TAG, "Could not make directory: " + LOCAL_APP_FOLDER);
        }
    }

    private void createCloudAppFolder() {
        try {
            cloudStorage.createFolder(CLOUD_APP_FOLDER);
        } catch (com.cloudrail.si.exceptions.HttpException hE) {
            Log.e(TAG, hE.getMessage());
        }
    }

    protected void prepareSession() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                atomicImageUploadCount = new AtomicInteger(0);
                compressionFormat = CompressFormat.JPEG;
                compressionQuality = 50;

                initCloudStorage();
                cloudStorageLogin();
                createAppFolder();
                CommandService.notifyCloudServicePrepared(getApplicationContext());
            }
        }).start();
    }

    protected void startSession() {
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
        sessionFolder = CLOUD_APP_FOLDER + "/" + getDateTime();

        try {
            cloudStorage.createFolder(sessionFolder);
        } catch (com.cloudrail.si.exceptions.HttpException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    private void createLocalSessionFolder() {
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
        } else if (compressionFormat == CompressFormat.JPEG) {
            extension = ".jpg";
        } else {
            Log.w(TAG, "Unknown compression format: " + compressionFormat);
            extension = null;
        }

        return extension;
    }

    protected int getUploadCount() {
        return atomicImageUploadCount.get();
    }

    protected void saveImage(final Bitmap bitmap) {
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
        try {
            byte[] byteArray = convertBitmapToByteArray(bitmap, compressionFormat, compressionQuality);

            File file = new File(path);

            if (file.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(byteArray);
                fos.flush();
                fos.close();
            } else {
                throw new IOException("Could not create new file " + path);
            }
        } catch (IOException iOE) {
            Log.e(TAG, "saveLocalImage: IOException: " + iOE.getMessage(), iOE);
            CommandService.notifyErrorSavingLocalImage(getApplicationContext());
        }
    }

    private void saveCloudImage(Bitmap bitmap, final String path) {
        ByteArrayInputStream byteArrayInputStream = convertDataToByteArrayInputStream(bitmap, compressionFormat, compressionQuality);

        try {
            cloudStorage.upload(path, byteArrayInputStream, byteArrayInputStream.available(), false);
        } catch (com.cloudrail.si.exceptions.HttpException ex) {
            //Grabbing images as fast as once a second sometimes causes name
            //conflicts. This fixes those.
            if (ex.getMessage().contains("same name")) {
                String pathB = path;
                if (compressionFormat == CompressFormat.JPEG) {
                    pathB = pathB.replace(".jpg", "");
                    pathB = pathB + "-b.jpg";
                } else if (compressionFormat == CompressFormat.PNG) {
                    pathB = pathB.replace(".png", "");
                    pathB = pathB + "-b.png";
                }
                saveCloudImage(bitmap, pathB);
            }
        }

        //Same as <int>++
        atomicImageUploadCount.getAndIncrement();
    }

    protected String getSessionCloudLink() {
        String link = null;
        if (cloudStorage != null) {
            link = cloudStorage.createShareLink(sessionFolder);
        }

        return link;
    }

    protected class CloudServiceBinder extends Binder {
        protected CloudService getService() {
            return CloudService.this;
        }
    }
}
