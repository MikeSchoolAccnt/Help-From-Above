package com.helpfromabove.helpfromabove;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

public class CloudPreferencesActivity extends HFAPreferenceActivity {
    private static final String TAG = "CloudPreferenceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.pref_cloud);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_cloud_storage_provider)));
    }
}
