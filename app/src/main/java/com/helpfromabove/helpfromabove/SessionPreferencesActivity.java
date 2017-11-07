package com.helpfromabove.helpfromabove;

import android.os.Bundle;
import android.util.Log;

public class SessionPreferencesActivity extends HFAPreferenceActivity {
    private static final String TAG = "SessionPrefe...Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.pref_session_start);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_uas_start_height)));
    }
}
