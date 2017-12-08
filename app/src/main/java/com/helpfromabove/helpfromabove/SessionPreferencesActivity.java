package com.helpfromabove.helpfromabove;

import android.os.Bundle;

public class SessionPreferencesActivity extends HFAPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.pref_session_start);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_uas_start_height)));
    }
}
