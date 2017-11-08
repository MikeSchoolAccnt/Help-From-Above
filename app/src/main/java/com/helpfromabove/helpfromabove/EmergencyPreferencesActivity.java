package com.helpfromabove.helpfromabove;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

public class EmergencyPreferencesActivity extends HFAPreferenceActivity {
    private static final String TAG = "EmergencyPre...Activity";

    private ArrayList<ContactInfo> contacts = new ArrayList<>();
    private ContactInfo emergencyContact = new ContactInfo("Local Emergency Dispatch", "911");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        this.addPreferencesFromResource(R.xml.pref_emergency);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_message_text)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_message_name)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_key_emergency_contacts)));

        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<ContactInfo> newContacts = new ArrayList<>();

                ContentResolver resolver = getContentResolver();
                Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

                while ((cursor != null) && (cursor.moveToNext())) {
                    String currentID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    Cursor phoneNumberCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{currentID}, null);

                    while ((phoneNumberCursor != null) && (phoneNumberCursor.moveToNext())) {
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        String number = phoneNumberCursor.getString(phoneNumberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        newContacts.add(new ContactInfo(name, number));
                    }
                    if (phoneNumberCursor != null) {
                        phoneNumberCursor.close();
                    }
                }

                if (cursor != null) {
                    cursor.close();
                }

                Collections.sort(newContacts);
                contacts = newContacts;

                //TODO : Enable this in final version. This is commented out so that we do not accidentally text during testing
//              contacts.add(0, emergencyContact);
                refreshContactsList();
            }
        }).start();
    }

    private void refreshContactsList() {
        Log.d(TAG, "refreshContactsList");
        try {
            MultiSelectListPreference contactsListPreference = (MultiSelectListPreference) findPreference(getString(R.string.pref_key_emergency_contacts));
            contactsListPreference.setEntries(getContactInfoCharSequenceArray());
            contactsListPreference.setEntryValues(getContactIdCharSequenceArray());
            contactsListPreference.notifyDependencyChange(true);
        } catch (IllegalStateException iSE) {
            Log.e(TAG, iSE.getLocalizedMessage(), iSE);
        }
    }

    private CharSequence[] getContactInfoCharSequenceArray() {
        Log.d(TAG, "getContactInfoCharSequenceArray");
        CharSequence[] contactCharSequences = new CharSequence[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            contactCharSequences[i] = contacts.get(i).toString();
        }

        return contactCharSequences;
    }

    private CharSequence[] getContactIdCharSequenceArray() {
        Log.d(TAG, "getContactIdCharSequenceArray");
        CharSequence[] contactCharSequences = new CharSequence[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            contactCharSequences[i] = contacts.get(i).number;
        }

        return contactCharSequences;
    }

    private class ContactInfo implements Comparable<ContactInfo> {
        private String name;
        private String number;

        ContactInfo(String _name, String _number) {
            name = _name;
            number = _number;
        }

        @Override
        public String toString() {
            return name + "\n" + number;
        }

        @Override
        public int compareTo(@NonNull ContactInfo contactInfo) {
            return this.name.compareTo(contactInfo.name);
        }
    }
}
