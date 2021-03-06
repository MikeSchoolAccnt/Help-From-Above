package com.helpfromabove.helpfromabove;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Caleb Smith on 11/7/17.
 * <p>
 * This is used to make sure the other preference activities are consistent.
 */

public class HFAPreferenceActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener = new SettingsOnPreferenceChangeListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);

    }

    protected void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

        if (preference instanceof MultiSelectListPreference) {
            bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getStringSet(preference.getKey(), new HashSet<String>()));
        } else {
            bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(preference.getKey(), ""));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        CommandService.notifySessionNotPrepared(getApplicationContext());
    }

    private class SettingsOnPreferenceChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(value.toString());
                CharSequence summary = index >= 0 ? listPreference.getEntries()[index] : "None";
                preference.setSummary(summary);
            } else if (preference instanceof MultiSelectListPreference) {
                Set<String> contactsSet = (Set<String>) value;
                CharSequence summary = contactsSet.isEmpty() ? "None" : contactsSet.toString();
                preference.setSummary(summary);
            } else {
                preference.setSummary(value.toString());
            }
            return true;
        }
    }
}
