package com.helpfromabove.helpfromabove;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private static final String ACTION_SENT = "com.helpfromabove.helpfromabove.action.ACTION_SENT";
    private static final String ACTION_DELIVERED = "com.helpfromabove.helpfromabove.action.ACTION_DELIVERED";
    private static final String EXTRA_NUMBER = "com.helpfromabove.helpfromabove.extra.EXTRA_NUMBER";
    private static final String EXTRA_MESSAGE = "com.helpfromabove.helpfromabove.extra.EXTRA_MESSAGE";

    private static PendingIntent sentPendingIntent;
    private static PendingIntent deliveredPindingIntent;

    private int totalMessageCount_SENT = 0;
    private int totalMessageCount_DELIVERED = 0;

    private SMSManagerBroadcastReceiver smsManagerBroadcastReceiver = new SMSManagerBroadcastReceiver();
    private final IBinder mBinder = new EmergencyServiceBinder();
    private IntentFilter smsManagerIntentFilter;
    public EmergencyService() {
        super();
        Log.d(TAG, "EmergencyService");
        smsManagerIntentFilter = new IntentFilter();
        smsManagerIntentFilter.addAction(ACTION_SENT);
        smsManagerIntentFilter.addAction(ACTION_DELIVERED);
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
        registerReceiver(smsManagerBroadcastReceiver, smsManagerIntentFilter);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(smsManagerBroadcastReceiver);
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
        } else {
            String emergencyMessage = getFormattedEmergencyMessage(location, cloudLink);
            sendSmsMessages(emergencyMessage);
        }
    }

    private void sendSmsMessages(String emergencyMessage) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> contactsSet = sharedPref.getStringSet(getString(R.string.pref_key_emergency_contacts), new HashSet<String>());

        // TODO : Uncomment after testing complete
//        totalMessageCount_SENT = contactsSet.size();
//        totalMessageCount_DELIVERED = contactsSet.size();
//        for (String emergencyContactNumber : contactsSet) {
//            Log.d(TAG, "emergencyContactNumber=" + emergencyContactNumber);
//            Log.d(TAG, "emergencyMessage= " + emergencyMessage);
//            sendSMSMessage(emergencyContactNumber, emergencyMessage);
//        }

        // TODO: Remove after testing complete
        totalMessageCount_SENT = 1;
        totalMessageCount_DELIVERED = 1;
        //sendSMSMessage("your number here", emergencyMessage);

    }

    private String getFormattedEmergencyMessage(Location location, String cloudLink) {
        Log.d(TAG, "getFormattedEmergencyMessage");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String name = sharedPref.getString(getString(R.string.pref_key_emergency_message_name), getString(R.string.pref_value_emergency_message_name_default));

        String emergencyMessage = sharedPref.getString(getString(R.string.pref_key_emergency_message_text), getString(R.string.pref_value_emergency_message_text_default));

        emergencyMessage = emergencyMessage.replace("(Name)", name);
        if (location != null) {
            emergencyMessage = emergencyMessage.replace("(Location)", "(" + location.getLatitude() + ", " + location.getLongitude() + ")");
        } else {
            emergencyMessage = emergencyMessage.replace("\nLocation: (Location)", "");
        }

        if (cloudLink != null) {
            emergencyMessage = emergencyMessage.replace("(Cloud Link)", cloudLink);
        } else {
            emergencyMessage = emergencyMessage.replace("\nImages: (Cloud Link)", "");
        }

        Log.w(TAG, emergencyMessage);
        return emergencyMessage;
    }

    private void sendSMSMessage(String recipientNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            sentPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SENT).putExtra(EXTRA_NUMBER,recipientNumber).putExtra(EXTRA_MESSAGE,message),0);
            deliveredPindingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DELIVERED).putExtra(EXTRA_NUMBER,recipientNumber).putExtra(EXTRA_MESSAGE,message),0);

            smsManager.sendTextMessage(recipientNumber, null, message, sentPendingIntent, deliveredPindingIntent);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "sendSMSMessage:" + e.getMessage());
        }
    }

    protected class EmergencyServiceBinder extends Binder {
        private static final String TAG = "EmergencyServiceBinder";

        protected EmergencyService getService() {
            Log.d(TAG, "getService");

            return EmergencyService.this;
        }
    }

    private class SMSManagerBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            switch (action){
                case ACTION_SENT:
                    if(getResultCode() == Activity.RESULT_OK){
                        totalMessageCount_SENT = totalMessageCount_SENT - 1;

                        //If all messages have been sent notify the CommandService
                        if(totalMessageCount_SENT == 0) {
                            CommandService.notifyEmergencyMessagesSent(getApplicationContext());
                        }
                    }
                    if(getResultCode() == Activity.RESULT_CANCELED){
                        //Only decrementing for testing. Maybe try to resend to the current number with x amount of tries
                        totalMessageCount_SENT = totalMessageCount_SENT - 1;

                        //How to resend
                        //Note: No way to tell how many times resending has been tried.

                        //String number = intent.getStringExtra(EXTRA_NUMBER);
                        //String msg = intent.getStringExtra(EXTRA_MESSAGE);
                        //sendSMSMessage(number,msg);
                    }
                    break;
                case ACTION_DELIVERED:
                    if(getResultCode() == Activity.RESULT_OK){
                        totalMessageCount_DELIVERED = totalMessageCount_DELIVERED - 1;

                        //If all messages have been delivered notify the CommandService
                        if(totalMessageCount_DELIVERED == 0) {
                            CommandService.notifyEmergencyMessagesDelivered(getApplicationContext());
                        }
                    }
                    if(getResultCode() == Activity.RESULT_CANCELED){
                        //Only decrementing for testing. Maybe try to resend to the current number with x amount of tries
                        totalMessageCount_DELIVERED = totalMessageCount_DELIVERED - 1;

                        //How to resend
                        //Note: No way to tell how many times resending has been tried.

                        //String number = intent.getStringExtra(EXTRA_NUMBER);
                        //String msg = intent.getStringExtra(EXTRA_MESSAGE);
                        //sendSMSMessage(number,msg);
                    }
                    break;

            }
        }
    }
}
