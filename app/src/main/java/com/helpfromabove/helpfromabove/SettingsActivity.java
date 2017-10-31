package com.helpfromabove.helpfromabove;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = "SettingsActivity";
    //This is needed for getting contact information.
    public static ContentResolver resolver;
    //This is used to keep the Emergency Contacts Setting screen after leaving it.
    //It also gives the ability to grab the data such as numbers from it.
    public static PreferenceScreen emergencyContactsPreferenceScreen;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
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
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        resolver = getContentResolver();
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        Log.d(TAG, "setupActionBar");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Log.d(TAG, "onMenuItemSelected");

        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || CloudPreferenceFragment.class.getName().equals(fragmentName)
                || EmergencyPreferenceFragment.class.getName().equals(fragmentName)
                || SessionStartPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows cloud preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CloudPreferenceFragment extends PreferenceFragment {
        private static final String TAG = "CloudPreferenceFragment";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate");
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_cloud);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_cloud_storage_provider)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Log.d(TAG, "onOptionsItemSelected");

            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows emergency sms preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class EmergencyPreferenceFragment extends PreferenceFragment {
        private static final String TAG = "EmergencyPreferenceF...";

        ArrayList<ContactInfo> contactInfos = new ArrayList<>();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate");
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_emergency);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_message_text)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_message_name)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Log.d(TAG, "onOptionsItemSelected");

            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onStart() {
            Log.d(TAG, "onStart");
            super.onStart();

            updateContactsArrayList();
            refreshContactsList();
        }

        private void refreshContactsList() {
            Log.d(TAG, "refreshContactsList");
            MultiSelectListPreference contactsListPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_emergency_contacts));
            contactsListPreference.setEntries(getContactInfoCharSequenceArray());
            contactsListPreference.setEntryValues(getContactIdCharSequenceArray());
        }

        private void updateContactsArrayList() {
            Log.d(TAG, "updateContactsArrayList");

            new Thread(new Runnable() {
                @Override
                public void run() {

                    ArrayList<ContactInfo> newContacts = new ArrayList<>();

                    ContentResolver resolver = SettingsActivity.resolver;
                    Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

                    while (cursor.moveToNext()) {
                        String currentID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor phoneNumberCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{currentID}, null);

                        while (phoneNumberCursor.moveToNext()) {
                            Long id = phoneNumberCursor.getLong(phoneNumberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            String number = phoneNumberCursor.getString(phoneNumberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
                            InputStream photo = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
                            newContacts.add(new ContactInfo(id, name, number, photo));
                        }
                    }

                    contactInfos = newContacts;
                    refreshContactsList();
                }
            }).start();
        }

        private CharSequence[] getContactInfoCharSequenceArray() {
            Log.d(TAG, "getContactInfoCharSequenceArray");
            CharSequence[] contactCharSequences = new CharSequence[contactInfos.size()];
            for (int i = 0; i < contactInfos.size(); i++) {
                contactCharSequences[i] = contactInfos.get(i).toString();
            }

            return contactCharSequences;
        }

        private CharSequence[] getContactIdCharSequenceArray() {
            Log.d(TAG, "getContactIdCharSequenceArray");
            CharSequence[] contactCharSequences = new CharSequence[contactInfos.size()];
            for (int i = 0; i < contactInfos.size(); i++) {
                contactCharSequences[i] = contactInfos.get(i).number;
            }

            return contactCharSequences;
        }

        private class ContactInfo {
            private Long contactId;
            private String name;
            private String number;
            private InputStream contactPhoto;

            ContactInfo(Long _contactId, String _name, String _number, InputStream _contactPhoto) {
                contactId = _contactId;
                name = _name;
                number = _number;
                contactPhoto = _contactPhoto;
            }

            @Override
            public String toString() {
                return name + "\n" + number;
            }
        }
    }

    /**
     * This fragment shows session start preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SessionStartPreferenceFragment extends PreferenceFragment {
        private static final String TAG = "SessionStartPreferen...";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate");
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_session_start);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_uas_start_height)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Log.d(TAG, "onOptionsItemSelected");

            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
