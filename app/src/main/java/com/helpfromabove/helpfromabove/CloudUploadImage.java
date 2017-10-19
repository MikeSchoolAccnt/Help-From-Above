package com.helpfromabove.helpfromabove;

import android.os.AsyncTask;
import android.util.Log;

import com.cloudrail.si.interfaces.CloudStorage;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Michael Purcell on 9/29/2017.
 */

public class CloudUploadImage extends AsyncTask<InputStream,Void,Void> {

    private static final String TAG = "CloudUploadImage";

    private CloudStorage cloudStorage;
    private String cloudSessionFolder;
    private String cloudAppFolder;
    private String fileName;

    public CloudUploadImage(CloudStorage cloudStorage, String cloudAppFolder, String cloudSessionFolder){
        this.cloudStorage = cloudStorage;
        this.cloudSessionFolder = cloudSessionFolder;
        this.cloudAppFolder = cloudAppFolder;
        this.fileName = fileName;
    }

    @Override
    protected Void doInBackground(InputStream... params) {

        Log.d(TAG,"Executing");

        fileName = getDateTime();

        InputStream inputStream = params[0];

        final String filePath = cloudSessionFolder + "/" + fileName + ".png";

        try {
            cloudStorage.upload(filePath, inputStream, inputStream.available(), false);
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }

        Log.d(TAG,"Done Uploading");
        return null;
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }
}
