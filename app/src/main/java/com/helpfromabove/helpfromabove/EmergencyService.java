package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
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
        for (String id : set) {
            Log.d(TAG, "startEmergency: emergencyContactId=" + id);
        }

    }

    protected class EmergencyServiceBinder extends Binder {
        private static final String TAG = "EmergencyServiceBinder";

        protected EmergencyService getService() {
            Log.d(TAG, "getService");

            return EmergencyService.this;
        }
    }
}
