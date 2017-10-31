package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

        checkPermissionAndSendSmsMessages(location, cloudLink);
    }

    private void checkPermissionAndSendSmsMessages(Location location, String cloudLink) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getApplicationContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.w(TAG, "checkPermissionAndSendSmsMessages: Permission to ACCESS_FINE_LOCATION is DENIED");
        }
        else {
            String emergencyMessage = getFormattedEmergencyMessage(location, cloudLink);
            sendSmsMessages(emergencyMessage);
        }
    }

    private void sendSmsMessages(String emergencyMessage) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> contactsSet = sharedPref.getStringSet(getString(R.string.pref_key_emergency_contacts), new HashSet<String>());

        // TODO : Uncomment after testing complete
//        for (String emergencyContactNumber : contactsSet) {
//            Log.d(TAG, "emergencyContactNumber=" + emergencyContactNumber);
//            Log.d(TAG, "emergencyMessage= " + emergencyMessage);
//            sendSMSMessage(emergencyContactNumber, emergencyMessage);
//        }

        // TODO: Remove after testing complete
//        sendSMSMessage("your number here", emergencyMessage);

    }

    private String getFormattedEmergencyMessage(Location location, String cloudLink) {
        Log.d(TAG, "getFormattedEmergencyMessage");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String name = sharedPref.getString(getString(R.string.pref_key_emergency_message_name), getString(R.string.pref_value_emergency_message_name_default));

        String emergencyMessage = sharedPref.getString(getString(R.string.pref_key_emergency_message_text), getString(R.string.pref_value_emergency_message_text_default));

        emergencyMessage = emergencyMessage.replace("(Name)", name);
        if (location != null) {
            emergencyMessage = emergencyMessage.replace("(Location)", "(" + location.getLatitude() + ", " + location.getLongitude() + ")");
        }
        else {
            emergencyMessage = emergencyMessage.replace("\nLocation: (Location)", "");
        }

        if (cloudLink != null) {
            emergencyMessage = emergencyMessage.replace("(Cloud Link)", cloudLink);
        }
        else {
            emergencyMessage = emergencyMessage.replace("\nImages: (Cloud Link)", "");
        }

        Log.w(TAG, emergencyMessage);
        return emergencyMessage;
    }

    private void sendSMSMessage(String recipientNumber, String message){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(recipientNumber, null, message, null, null);
            CommandService.notifyEmergencyMessageSent(getApplicationContext());
        } catch (IllegalArgumentException e){
            Log.e(TAG,"sendSMSMessage:" + e.getMessage());
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
