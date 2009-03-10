package org.mortbay.ijetty.console;

import org.mortbay.ijetty.console.DatabaseCollection;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Contacts;

public class Phone
{  
    
    static final String[] phonesProjection = 
        new String[] 
                   {
                       android.provider.BaseColumns._ID,
                       android.provider.Contacts.PhonesColumns.LABEL,
                       android.provider.Contacts.PhonesColumns.NUMBER,
                       android.provider.Contacts.PhonesColumns.NUMBER_KEY,
                       android.provider.Contacts.PhonesColumns.TYPE 
                   };

    public static class PhoneCollection extends DatabaseCollection
    {
        public PhoneCollection (Cursor cursor)
        {
            super(cursor);
        }
        
        public ContentValues cursorToValues(Cursor cursor)
        {
            return cursorToPhoneValues(cursor);
        }
    }


    /**
     * getPhones
     * 
     * Get the phone numbers for a Contact.
     * 
     * @param resolver
     * @param userId the Contact whose phones to return
     * @return
     */
    public static PhoneCollection getPhones (ContentResolver resolver, String userId)
    {
        if (userId == null)
            return null;
        
        String[] whereArgs = new String[]{userId};
        return new PhoneCollection (resolver.query(Contacts.Phones.CONTENT_URI, phonesProjection, 
                "people."+android.provider.BaseColumns._ID+" = ?", 
                whereArgs, Contacts.PhonesColumns.TYPE+" ASC"));
    }
    
    
    public static void addPhone (ContentResolver resolver, ContentValues phone, String userId)
    {
        Uri peopleUri = Uri.withAppendedPath(Contacts.People.CONTENT_URI, userId);
        resolver.insert(Uri.withAppendedPath(peopleUri, Contacts.People.Phones.CONTENT_DIRECTORY), phone);
    }
    
    
    public static void deletePhone (ContentResolver resolver, String phoneId, String userId)
    {
        resolver.delete(Uri.withAppendedPath (Contacts.Phones.CONTENT_URI, phoneId),null, null);
    }
    
    public static void savePhone (ContentResolver resolver, ContentValues phone, String phoneId, String userId)
    {
        Uri uri = Uri.withAppendedPath(Contacts.Phones.CONTENT_URI, phoneId);
        resolver.update (uri, phone, null, null);
    }
    
    
    private static ContentValues cursorToPhoneValues (Cursor cursor)
    {
        if (cursor == null)
            return null;
        
        ContentValues values = new ContentValues();
        String val;
        val = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));
        values.put(android.provider.BaseColumns._ID, val);
        
        val =  cursor.getString(cursor.getColumnIndex(Contacts.PhonesColumns.LABEL));
        values.put(Contacts.PhonesColumns.LABEL, val);
        
        val = cursor.getString(cursor.getColumnIndex(Contacts.PhonesColumns.NUMBER));
        values.put (Contacts.PhonesColumns.NUMBER, val);
        
        Integer intVal = new Integer(cursor.getInt(cursor.getColumnIndex(Contacts.PhonesColumns.TYPE)));
        values.put(Contacts.PhonesColumns.TYPE, intVal);
        
        return values;
    }
}
