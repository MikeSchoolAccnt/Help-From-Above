package com.helpfromabove.helpfromabove;

import android.os.Bundle;

public class CloudPreferencesActivity extends HFAPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.pref_cloud);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_cloud_storage_provider)));
    }
}
