package com.kitapp.custom;

/**
 * From https://github.com/rt2zz/react-native-contacts
 * Modified by cholland on 10/7/16.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;

import static android.provider.ContactsContract.CommonDataKinds.*;

public class ContactsProvider {
    public static final int ID_FOR_PROFILE_CONTACT = -1;

    private static final List<String> JUST_ME_PROJECTION = new ArrayList<String>() {{
        add(ContactsContract.Contacts.Data.MIMETYPE);
        add(ContactsContract.Profile.DISPLAY_NAME);
//        add(Contactables.PHOTO_URI);
        add(StructuredName.DISPLAY_NAME);
        add(StructuredName.GIVEN_NAME);
        add(StructuredName.MIDDLE_NAME);
        add(StructuredName.FAMILY_NAME);
        add(StructuredPostal.STREET);
        add(StructuredPostal.CITY);
        add(StructuredPostal.REGION);
        add(StructuredPostal.POSTCODE);
        add(StructuredPostal.COUNTRY);
        add(StructuredPostal.TYPE);
        add(StructuredPostal.FORMATTED_ADDRESS);
        add(Phone.NUMBER);
        add(Phone.TYPE);
        add(Phone.LABEL);
        add(Email.DATA);
        add(Email.ADDRESS);
        add(Email.TYPE);
        add(Email.LABEL);
    }};

    private static final List<String> FULL_PROJECTION = new ArrayList<String>() {{
        add(ContactsContract.Data.CONTACT_ID);
        add(ContactsContract.RawContacts.SOURCE_ID);
        addAll(JUST_ME_PROJECTION);
    }};

    // Search selection stuff
    // Defines the text expression
    @SuppressLint("InlinedApi")
    private static final String SELECTION =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?" :
                    ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";


    private final ContentResolver contentResolver;
    private final Context context;

    public ContactsProvider(ContentResolver contentResolver, Context context) {
        this.contentResolver = contentResolver;
        this.context = context;
    }

    public WritableArray getMatchingContacts(String mSearchString) {
        String[] mSelectionArgs = {mSearchString};
        mSelectionArgs[0] = "%" + mSearchString + "%";

        Map<String, Contact> everyoneElse;
        {
            Cursor cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    FULL_PROJECTION.toArray(new String[FULL_PROJECTION.size()]),
                    SELECTION,
                    mSelectionArgs,
                    null
            );

            try {
                everyoneElse = loadContactsFrom(cursor);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        WritableArray contacts = Arguments.createArray();
        for (Contact contact : everyoneElse.values()) {
            contacts.pushMap(contact.toMap());
        }

        return contacts;
    }

    public WritableArray getContacts() {
        Map<String, Contact> justMe;
        {
            Cursor cursor = contentResolver.query(
                    Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                    JUST_ME_PROJECTION.toArray(new String[JUST_ME_PROJECTION.size()]),
                    null,
                    null,
                    null
            );

            try {
                justMe = loadContactsFrom(cursor);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        Map<String, Contact> everyoneElse;
        {
            Cursor cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    FULL_PROJECTION.toArray(new String[FULL_PROJECTION.size()]),
                    ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?",
                    new String[]{Email.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, StructuredName.CONTENT_ITEM_TYPE},
                    null
            );

            try {
                everyoneElse = loadContactsFrom(cursor);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        WritableArray contacts = Arguments.createArray();
        for (Contact contact : justMe.values()) {
            contacts.pushMap(contact.toMap());
        }
        for (Contact contact : everyoneElse.values()) {
            contacts.pushMap(contact.toMap());
        }

        return contacts;
    }

    @NonNull
    private Map<String, Contact> loadContactsFrom(Cursor cursor) {

        Map<String, Contact> map = new LinkedHashMap<>();

        while (cursor != null && cursor.moveToNext()) {

            int columnIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
            String contactId;
            if (columnIndex != -1) {
                contactId = String.valueOf(cursor.getInt(columnIndex));
            } else {
                contactId = String.valueOf(ID_FOR_PROFILE_CONTACT);//no contact id for 'ME' user
            }

            columnIndex = cursor.getColumnIndex(ContactsContract.RawContacts.SOURCE_ID);
            if (columnIndex != -1) {
                String uid = cursor.getString(columnIndex);
                if (uid != null) {
                    contactId = uid;
                }
            }

            if (!map.containsKey(contactId)) {
                map.put(contactId, new Contact(contactId));
            }

            Contact contact = map.get(contactId);

            String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));

            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            if (!TextUtils.isEmpty(name) && TextUtils.isEmpty(contact.displayName)) {
                contact.displayName = name;
            }

//            String rawPhotoURI = cursor.getString(cursor.getColumnIndex(Contactables.PHOTO_URI));
//            if (!TextUtils.isEmpty(rawPhotoURI)) {
//                contact.photoUri = getPhotoURIFromContactURI(rawPhotoURI, contactId);
//            }

            if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                contact.givenName = cursor.getString(cursor.getColumnIndex(StructuredName.GIVEN_NAME));
                contact.middleName = cursor.getString(cursor.getColumnIndex(StructuredName.MIDDLE_NAME));
                contact.familyName = cursor.getString(cursor.getColumnIndex(StructuredName.FAMILY_NAME));
            } else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                int type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));

                if (!TextUtils.isEmpty(phoneNumber)) {
                    String label;
                    switch (type) {
                        case Phone.TYPE_HOME:
                            label = "home";
                            break;
                        case Phone.TYPE_WORK:
                            label = "work";
                            break;
                        case Phone.TYPE_MOBILE:
                            label = "mobile";
                            break;
                        default:
                            label = "other";
                    }
                    contact.phones.add(new Contact.Item(label, phoneNumber));
                }
            } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                String email = cursor.getString(cursor.getColumnIndex(Email.ADDRESS));
                int type = cursor.getInt(cursor.getColumnIndex(Email.TYPE));

                if (!TextUtils.isEmpty(email)) {
                    String label;
                    switch (type) {
                        case Email.TYPE_HOME:
                            label = "home";
                            break;
                        case Email.TYPE_WORK:
                            label = "work";
                            break;
                        case Email.TYPE_MOBILE:
                            label = "mobile";
                            break;
                        case Email.TYPE_CUSTOM:
                            if(cursor.getString(cursor.getColumnIndex(Email.LABEL)) != null){
                                label = cursor.getString(cursor.getColumnIndex(Email.LABEL)).toLowerCase();
                            } else {
                                label = "";
                            }
                            break;
                        default:
                            label = "other";
                    }
                    contact.emails.add(new Contact.Item(label, email));
                }
            } else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                int type = cursor.getInt(cursor.getColumnIndex(StructuredPostal.TYPE));
                WritableMap postal = Arguments.createMap();
                String formattedAddress
                        = cursor.getString(
                        cursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS));
                String street = cursor.getString(cursor.getColumnIndex(StructuredPostal.STREET));
                String city = cursor.getString(cursor.getColumnIndex(StructuredPostal.CITY));
                String region = cursor.getString(cursor.getColumnIndex(StructuredPostal.REGION));
                String postcode = cursor.getString(cursor.getColumnIndex(StructuredPostal.POSTCODE));
                String country = cursor.getString(cursor.getColumnIndex(StructuredPostal.COUNTRY));
                postal.putString("street", street);
                postal.putString("city", city);
                postal.putString("region", region);
                postal.putString("postcode", postcode);
                postal.putString("country", country);

                if (!TextUtils.isEmpty(street)) {
                    String label;
                    switch (type) {
                        case StructuredPostal.TYPE_HOME:
                            label = "home";
                            break;
                        case StructuredPostal.TYPE_WORK:
                            label = "work";
                            break;
                        case StructuredPostal.TYPE_OTHER:
                            label = "other";
                            break;
                        default:
                            label = "other";
                    }
                    contact.postals.add(new Contact.Item(label, formattedAddress, postal));
                }
            }
        }

        return map;
    }

    private String getPhotoURIFromContactURI(String contactURIString, String contactId) {
        String photoURI = "";
        try {
            Uri contactURI = Uri.parse(contactURIString);
            InputStream photoStream = contentResolver.openInputStream(contactURI);
            BufferedInputStream in = new BufferedInputStream(photoStream);
            File outputDir = context.getCacheDir(); // context being the Activity pointer
            File outputFile = File.createTempFile("contact" + contactId, ".jpg", outputDir);
            FileOutputStream output = new FileOutputStream(outputFile);

            int count = 0;
            byte[] buffer = new byte[4098];

            while ((count = in.read(buffer)) > 0)
            {
                output.write(buffer, 0, count);
            }

            photoURI = "file://" + outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("ContactsProvider", "Error writing contact image to file:", e);
        }

        return photoURI;
    }

    private static class Contact {
        private String contactId;
        private String displayName;
        private String givenName = "";
        private String middleName = "";
        private String familyName = "";
        // private String photoUri;
        private List<Item> emails = new ArrayList<>();
        private List<Item> phones = new ArrayList<>();
        private List<Item> postals = new ArrayList<>();

        public Contact(String contactId) {
            this.contactId = contactId;
        }

        public WritableMap toMap() {
            WritableMap contact = Arguments.createMap();
            contact.putString("recordID", contactId);
            contact.putString("givenName", TextUtils.isEmpty(givenName) ? displayName : givenName);
            contact.putString("middleName", middleName);
            contact.putString("familyName", familyName);
            // contact.putString("thumbnailPath", photoUri == null ? "" : photoUri);

            WritableArray phoneNumbers = Arguments.createArray();
            for (Item item : phones) {
                WritableMap map = Arguments.createMap();
                map.putString("number", item.value);
                map.putString("label", item.label);
                phoneNumbers.pushMap(map);
            }
            contact.putArray("phoneNumbers", phoneNumbers);

            WritableArray emailAddresses = Arguments.createArray();
            for (Item item : emails) {
                WritableMap map = Arguments.createMap();
                map.putString("email", item.value);
                map.putString("label", item.label);
                emailAddresses.pushMap(map);
            }
            contact.putArray("emailAddresses", emailAddresses);

            WritableArray postalAddresses = Arguments.createArray();
            for (Item item : postals) {
                WritableMap map = Arguments.createMap();
                map.putMap("address", item.values);
                map.putString("formattedAddress", item.value);
                map.putString("label", item.label);
                postalAddresses.pushMap(map);
            }
            contact.putArray("postalAddresses", postalAddresses);

            return contact;
        }

        public static class Item {
            public String label;
            public String value;
            public WritableMap values;

            public Item(String label, String value) {
                this.label = label;
                this.value = value;
            }

            public Item(String label, String value, WritableMap values) {
                this.label = label;
                this.value = value;
                this.values = values;
            }
        }
    }
}
