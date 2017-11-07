package com.helpfromabove.helpfromabove;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;

/**
 * Created by csmith on 11/7/17.
 *
 * This is used to make sure the other preference activities are consistent.
 */

public class HFAPreferenceActivity extends AppCompatPreferenceActivity{
    private static final String TAG = "HFAPreferenceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        Log.d(TAG, "setupActionBar");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new SettingsOnPreferenceChangeListener();

    protected static void bindPreferenceSummaryToValue(Preference preference) {
        Log.d(TAG, "bindPreferenceSummaryToValue");

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    static class SettingsOnPreferenceChangeListener implements  Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.d(TAG, "onPreferenceChange: preference.getKey()=" + preference.getKey() + ", value.toString()=" + value.toString());

            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    }

}
