package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class EmergencyService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "EmergencyService";

    private final IBinder mBinder = new EmergencyServiceBinder();

    public EmergencyService() {
        super();

        Log.d(TAG, "EmergencyService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

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
        if (key.equals(getString(R.string.pref_key_emergency_message_name))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_emergency_message_name");
        } else if (key.equals(getString(R.string.pref_key_emergency_message_text))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_emergency_message_text");
        } else if (key.equals(getString(R.string.pref_key_emergency_contacts))) {
            Log.d(TAG, "onSharedPreferenceChanged: pref_key_emergency_contacts");
        }
    }

    protected void startEmergency(Location location, String cloudLink) {
        Log.d(TAG, "startEmergency: location=" + location + ", cloudLink=" + cloudLink);

        PreferenceScreen screen = SettingsActivity.emergencyContactsPreferenceScreen;

        if(screen == null){
            Log.d(TAG, "startEmergency: Implement a way to grab contacts is not already done.");
        }
        //Lists all contacts that are checked as active. Can be used later for making messages.
        else {
            for (int i = 0; i < screen.getPreferenceCount(); i++){
                CheckBoxPreference box = (CheckBoxPreference) screen.getPreference(i);

                if(box.isChecked()) {
                    Log.d(TAG, box.getTitle() + " : " + box.getSummary());
                }
            }
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> set = sharedPref.getStringSet(getString(R.string.pref_key_emergency_contacts), new HashSet<String>());

        String userName = sharedPref.getString(getString(R.string.pref_key_emergency_message_name), getString(R.string.pref_default_emergency_message_name));
        String emergencyMessage = sharedPref.getString(getString(R.string.pref_key_emergency_message_text), getString(R.string.pref_value_emergency_message_text_default));

        //TODO: replace the default GPS string and CloudLink String in third emergency_message_text
        emergencyMessage = emergencyMessage.replace("(Name)",userName);

        for (String number : set) {
            Log.d(TAG, "emergencyContactNumber=" + number);
            Log.d(TAG, "emergencyMessage= " + emergencyMessage);
            //sendSMSMessage(number,emergencyMessage);
        }

        //Input your own information here for testing
        //sendSMSMessage("Your test number",emergencyMessage);
    }

    //Allows for sending a message to someone through the application
    //The message passed in this might not be needed instead it can be grabbed from shared preferences.
    //TODO: Figure a way to request permisions since this is not an activity.
    private void sendSMSMessage(String recipientNumber, String message){
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            int response = getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
//            if (response == PackageManager.PERMISSION_GRANTED) {
                //This try catch is only for if an invalid number or message is trying to be sent.
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(recipientNumber, null, message, null, null);
                } catch (IllegalArgumentException e){
                    Log.e(TAG,"sendSMSMessage:" + e.getMessage());
                }
//            } else {
//                  ActivityCompat.requestPermissions("Need Activity Here", new String[]{android.Manifest.permission.SEND_SMS}, 0);
//            }
//        }
    }

    protected class EmergencyServiceBinder extends Binder {
        private static final String TAG = "EmergencyServiceBinder";

        protected EmergencyService getService() {
            Log.d(TAG, "getService");

            return EmergencyService.this;
        }
    }
}
