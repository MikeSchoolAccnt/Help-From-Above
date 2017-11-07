package com.helpfromabove.helpfromabove;

import android.os.Bundle;
import android.util.Log;

public class EmergencyPreferencesActivity extends HFAPreferenceActivity {
    private static final String TAG = "EmergencyPre...Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.pref_emergency);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_message_text)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_message_name)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_contacts)));
    }
}
