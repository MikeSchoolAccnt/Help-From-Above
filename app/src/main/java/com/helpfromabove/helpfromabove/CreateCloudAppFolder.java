package com.helpfromabove.helpfromabove;

import android.os.AsyncTask;
import android.util.Log;

import com.cloudrail.si.interfaces.CloudStorage;

/**
 * Created by Michael Purcell on 9/29/2017.
 */

public class CreateCloudAppFolder extends AsyncTask<Void,Void,String> {

    private static final String TAG = "CreateCloudAppFolder";

    private CloudStorage cloudStorage;
    private String cloudAppFolder;

    CreateCloudAppFolder(CloudStorage cloudStorage, String cloudAppFolder){
        this.cloudStorage = cloudStorage;
        this.cloudAppFolder = cloudAppFolder;

    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG,"Executing");

        try{
            cloudStorage.createFolder(cloudAppFolder);
        } catch (com.cloudrail.si.exceptions.HttpException ex){
            Log.e(TAG,ex.getMessage());
        }

        return null;
    }
}
