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

public class EmergencyService extends Service {
    private static final String TAG = "EmergencyService";

    private static final String ACTION_SENT = "com.helpfromabove.helpfromabove.action.ACTION_SENT";
    private static final String ACTION_DELIVERED = "com.helpfromabove.helpfromabove.action.ACTION_DELIVERED";
    private static final String EXTRA_NUMBER = "com.helpfromabove.helpfromabove.extra.EXTRA_NUMBER";
    private static final String EXTRA_MESSAGE = "com.helpfromabove.helpfromabove.extra.EXTRA_MESSAGE";
    private final IBinder mBinder = new EmergencyServiceBinder();
    private int totalMessageCount_SENT = 0;
    private int totalMessageCount_DELIVERED = 0;
    private SMSManagerBroadcastReceiver smsManagerBroadcastReceiver = new SMSManagerBroadcastReceiver();
    private IntentFilter smsManagerIntentFilter;

    public EmergencyService() {
        super();
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
        super.onCreate();
        registerReceiver(smsManagerBroadcastReceiver, smsManagerIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsManagerBroadcastReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void startEmergency(Location location, String cloudLink) {
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

        if (contactsSet.isEmpty()) {
            Log.i(TAG, "No emergency contacts selected. Ending emergency.");
            CommandService.notifySessionEmergencyEnd(getApplicationContext());
        }
        else {
            totalMessageCount_SENT = contactsSet.size();
            totalMessageCount_DELIVERED = contactsSet.size();
            for (String emergencyContactNumber : contactsSet) {
                sendSMSMessage(emergencyContactNumber, emergencyMessage);
            }
        }
    }

    private String getFormattedEmergencyMessage(Location location, String cloudLink) {
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

        Log.w(TAG, "emergencyMessage=" + emergencyMessage);
        return emergencyMessage;
    }

    private void sendSMSMessage(String recipientNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_SENT).putExtra(EXTRA_NUMBER, recipientNumber).putExtra(EXTRA_MESSAGE, message), 0);
            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_DELIVERED).putExtra(EXTRA_NUMBER, recipientNumber).putExtra(EXTRA_MESSAGE, message), 0);

            smsManager.sendTextMessage(recipientNumber, null, message, sentPendingIntent, deliveredPendingIntent);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "sendSMSMessage:" + e.getMessage());
        }
    }

    protected class EmergencyServiceBinder extends Binder {
        protected EmergencyService getService() {
            return EmergencyService.this;
        }
    }

    private class SMSManagerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_SENT:
                        switch (getResultCode()) {
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            case SmsManager.RESULT_ERROR_RADIO_OFF:
                            case SmsManager.RESULT_ERROR_NULL_PDU:
                                String number = intent.getStringExtra(EXTRA_NUMBER);
                                Log.w(TAG, "Error sending SMS message to " + number);
                            case Activity.RESULT_OK:
                                totalMessageCount_SENT = totalMessageCount_SENT - 1;

                                //If all messages have been sent notify the CommandService
                                if (totalMessageCount_SENT == 0) {
                                    CommandService.notifyEmergencyMessagesSent(getApplicationContext());
                                    CommandService.notifySessionEmergencyEnd(getApplicationContext());
                                }
                                break;
                            default:
                                Log.w(TAG, "onReceive: default: action=" + action);
                                break;
                        }
                        break;
                    case ACTION_DELIVERED:
                        totalMessageCount_DELIVERED = totalMessageCount_DELIVERED - 1;

                        //If all messages have been delivered notify the CommandService
                        if (totalMessageCount_DELIVERED == 0) {
                             CommandService.notifyEmergencyMessagesDelivered(getApplicationContext());
                        }
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
