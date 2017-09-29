package com.helpfromabove.helpfromabove;

import android.os.AsyncTask;
import android.util.Log;

import com.cloudrail.si.interfaces.CloudStorage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Michael Purcell on 9/29/2017.
 */

public class CreateCloudSessionFolder extends AsyncTask<Void,String,String> {

    private static final String TAG = "CreateCloudSessionFolde";

    private CloudStorage cloudStorage;
    private String cloudAppFolder;

    private String cloudSessionFolder;

    public CreateCloudSessionFolder(CloudStorage cloudStorage, String cloudAppFolder){
        this.cloudStorage = cloudStorage;
        this.cloudAppFolder = cloudAppFolder;
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG,"Executing");

        cloudSessionFolder = cloudAppFolder + "/" + getDateTime();

        //This try catch will most likely never get used but better to be safe.
        try {
            cloudStorage.createFolder(cloudSessionFolder);
        } catch (com.cloudrail.si.exceptions.HttpException ex) {
            Log.e(TAG,ex.getMessage());
        }

        return cloudSessionFolder;
    }

    private String getDateTime() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZZZ", Locale.getDefault());
        return df.format(Calendar.getInstance().getTime());
    }

    @Override
    protected void onPostExecute(String s) {

        //Might need to change to brodcasting the cloudSessionFolder name
        //depending on how quickly it needs accessed.
        super.onPostExecute(s);

    }
}
