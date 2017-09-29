package com.helpfromabove.helpfromabove;

import android.os.AsyncTask;
import android.util.Log;

import com.cloudrail.si.interfaces.CloudStorage;

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

    public CloudUploadImage(CloudStorage cloudStorage, String cloudAppFolder, String cloudSessionFolder, String fileName){
        this.cloudStorage = cloudStorage;
        this.cloudSessionFolder = cloudSessionFolder;
        this.cloudAppFolder = cloudAppFolder;
        this.fileName = fileName;
    }

    @Override
    protected Void doInBackground(InputStream... params) {

        Log.d(TAG,"Executing");

        InputStream inputStream = params[0];

        final String filePath = cloudSessionFolder + "/" + fileName;

        cloudStorage.upload(filePath, inputStream, 0, false);

        return null;
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }
}
