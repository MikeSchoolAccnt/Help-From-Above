package com.helpfromabove.helpfromabove;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

/**
 * Created by Michael Purcell on 5/18/2017.
 */

public class SettingsActivity extends AppCompatActivity {

    private final String TAG = "SettingsActivity";

    //Possibly need for later on when changing other settings
    private SettingsBroadcastReceiver sBR = new SettingsBroadcastReceiver();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_);
        final Activity current = this;
        final RadioGroup options = (RadioGroup) findViewById(R.id.radioGroup_CLOUDS);
        Button connection = (Button) findViewById(R.id.button_connect);


        connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checked = options.getCheckedRadioButtonId();
                Intent cloud = new Intent(CommandService.SETTING_CHANGE_CLOUD);

                if(checked == R.id.radio_DROPBOX) {
                    cloud.putExtra(CommandService.EXTRA_CLOUD_TYPE,CommandService.CONSTANT_CLOUD_DROPBOX);
                    sendBroadcast(cloud);
                } else if (checked == R.id.radio_GOOGLEDRIVE) {
                    cloud.putExtra(CommandService.EXTRA_CLOUD_TYPE,CommandService.CONSTANT_CLOUD_GOOGLE_DRIVE);
                    sendBroadcast(cloud);
                } else if (checked == R.id.radio_ONEDRIVE) {
                    cloud.putExtra(CommandService.EXTRA_CLOUD_TYPE,CommandService.CONSTANT_CLOUD_ONE_DRIVE);
                    sendBroadcast(cloud);
                } else {
                    /*
                     * No valid Cloud Service selected (Not sure if possible)
                     * Cloud also be where internal storage is specified
                     */
                    Log.d(TAG, "Error: Invalid Cloud Service was selected");
                }
            }
        });
    }



    private class SettingsBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case CommandService.SETTING_CHANGE_CLOUD:

                        break;

                }
            }
        }
    }

}
