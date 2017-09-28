package com.helpfromabove.helpfromabove;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cloudrail.si.interfaces.CloudStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Michael Purcell on 9/28/2017.
 */

public class AsyncCloudTask extends AsyncTask<String,String,Void> {

    protected static final String CREATE_CLOUD_APP_FOLDER = "com.helpfromabove.helpfromabove.CREATE_CLOUD_APP_FOLDER";
    protected static final String CREATE_CLOUD_SESSION_FOLDER = "com.helpfromabove.helpfromabove.CREATE_CLOUD_SESSION_FOLDER";
    protected static final String CLOUD_UPLOAD_IMAGE = "com.helpfromabove.helpfromabove.CLOUD_UPLOAD_IMAGE";
    protected static final String GET_SESSION_CLOUD_LINK = "com.helpfromabove.helpfromabove.GET_SESSION_CLOUD_LINK"; //Might not need this

    private final String TAG = "AsyncCloudTask";
    private CloudStorage cloudStorage;

    public AsyncCloudTask(CloudStorage cloudStorage){
        this.cloudStorage = cloudStorage;
    }

    @Override
    protected Void doInBackground(String... params) {

        String cloudAppFolder;
        String cloudSessionFolder;
        switch (params[0]){
            case CREATE_CLOUD_APP_FOLDER:
                Log.d(TAG,"CREATE_CLOUD_APP_FOLDER");

                cloudAppFolder = params[1];
                try{
                    cloudStorage.createFolder(cloudAppFolder);
                } catch (com.cloudrail.si.exceptions.HttpException ex){
                    Log.e(TAG,ex.getMessage());
                }

                break;
            case CREATE_CLOUD_SESSION_FOLDER:
                Log.d(TAG,"CREATE_CLOUD_SESSION_FOLDER");

                cloudAppFolder = params[1];
                cloudSessionFolder = cloudAppFolder + "/" + getDateTime();
                try {
                    cloudStorage.createFolder(cloudSessionFolder);
                } catch (com.cloudrail.si.exceptions.HttpException ex) {
                    Log.e(TAG,ex.getMessage());
                }
                break;
//            TODO: Implement this when the InputStream of the image can be converted into a string.
//            case CLOUD_UPLOAD_IMAGE:
//                Log.d(TAG,"CLOUD_UPLOAD_IMAGE");
//                cloudAppFolder = params[1];
//                cloudSessionFolder = cloudAppFolder + "/" + getDateTime();
//                String filename = params[2];
//                String inputSteamAsString = params[3];
//                InputStream inputStream = new ByteArrayInputStream(inputSteamAsString.getBytes(StandardCharsets.UTF_8));
//                final String filePath = cloudSessionFolder + "/" + filename;
//                cloudStorage.upload(filePath, inputStream, 0, false);
//                break;
            default:
                Log.e(TAG,"Invalid Arguments");
                break;


        }

        return null;
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }
}
