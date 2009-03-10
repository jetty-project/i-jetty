package org.mortbay.ijetty.console;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;

public class User
{
  
    /**
     * UserCollection
     *
     * Inner class wrapping a Cursor over Contacts
     */
    public static class UserCollection extends DatabaseCollection
    {
        public UserCollection (Cursor cursor)
        {
            super(cursor);
        }

        public ContentValues cursorToValues(Cursor cursor)
        {
           return cursorToUserValues(cursor);
        }
       
    }
    
  
    
    public static class ContactMethodsCollection extends DatabaseCollection
    {
        public ContactMethodsCollection(Cursor cursor)
        {
            super(cursor);
        }    
        public ContentValues cursorToValues(Cursor cursor)
        {
            if (cursor == null)
                return null;
            
            ContentValues values = new ContentValues();
            String val;
            val = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.DATA));
            values.put(Contacts.ContactMethodsColumns.DATA, val);
            val = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.AUX_DATA));
            values.put(Contacts.ContactMethodsColumns.AUX_DATA,val);
            val = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.LABEL));
            values.put(Contacts.ContactMethodsColumns.LABEL, val);
            Integer intVal = new Integer(cursor.getInt(cursor.getColumnIndex(Contacts.ContactMethodsColumns.ISPRIMARY)));
            values.put(Contacts.ContactMethodsColumns.ISPRIMARY, intVal);
            intVal = new Integer(cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.KIND)));
            values.put(Contacts.ContactMethodsColumns.KIND, intVal);
            intVal = new Integer(cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.TYPE)));
            values.put(Contacts.ContactMethodsColumns.TYPE, intVal);
            return values;
        }
    }

    
    static final String[] baseProjection = 
        new String[] 
                   {
                       android.provider.BaseColumns._ID,
                       android.provider.Contacts.PeopleColumns.DISPLAY_NAME,
                       android.provider.Contacts.PeopleColumns.NOTES,
                       android.provider.Contacts.PeopleColumns.STARRED
                   };
    
    static final String[] contactMethodsProjection = 
        new String[] 
                   {
                       android.provider.BaseColumns._ID,
                       android.provider.Contacts.ContactMethodsColumns.DATA,
                       android.provider.Contacts.ContactMethodsColumns.AUX_DATA,
                       android.provider.Contacts.ContactMethodsColumns.KIND,
                       android.provider.Contacts.ContactMethodsColumns.LABEL,
                       android.provider.Contacts.ContactMethodsColumns.TYPE,
                       android.provider.Contacts.ContactMethodsColumns.ISPRIMARY
                   };


    
    
    /**
     * create
     * 
     * Create a new Contact.
     * 
     * @param resolver
     * @param values
     * @return
     */
    public static String create (ContentResolver resolver, ContentValues values)
    {
        if (resolver == null)
            return null;
        if (values == null)
            return null;
        Uri uri = Contacts.People.createPersonInMyContactsGroup(resolver, values);
        //Uri uri = resolver.insert(Contacts.People.CONTENT_URI, values);
        return  String.valueOf(ContentUris.parseId(uri));
    }
    
    
    
    /**
     * save
     * 
     * Update an existing Contact.
     * 
     * @param resolver
     * @param values
     * @param id
     */
    public static void save (ContentResolver resolver, ContentValues values, String id)
    {
        if (resolver == null)
            return;
        if (values == null)
            return;
        if (id == null)
            return;
        Uri uri = Uri.withAppendedPath(Contacts.People.CONTENT_URI, id);
        resolver.update (uri, values, null, null);
    }
    
    
    
    /**
     * delete
     * 
     * Delete a Contact.
     * 
     * @param resolver
     * @param id
     */
    public static void delete (ContentResolver resolver, String id)
    {
        if (id == null)
            return;
        
        resolver.delete(Uri.withAppendedPath(Contacts.People.CONTENT_URI, id), null, null);
    }
    
      
    
    /**
     * getAll
     * 
     * Get all Contacts.
     * 
     * @param resolver
     * @return
     */
    public static UserCollection getAll (ContentResolver resolver)
    {
        return new UserCollection(resolver.query(Contacts.People.CONTENT_URI, baseProjection, null, null, null)); 
    }
    
    
    
    /**
     * get
     * 
     * Get a single Contact.
     * 
     * @param resolver
     * @param id
     * @return
     */
    public static ContentValues get (ContentResolver resolver, String id)
    {
        if (id == null)
            return null;
        
        String[] whereArgs = new String[]{id};
        Cursor cursor = resolver.query(Contacts.People.CONTENT_URI, baseProjection, 
                "people."+android.provider.BaseColumns._ID+" = ?", 
                whereArgs, Contacts.PeopleColumns.NAME+" ASC");
        cursor.moveToFirst();
        ContentValues values =  cursorToUserValues(cursor);
        cursor.close();
        return values;
    }

    
    
    /**
     * getContactMethods
     * 
     * Get the ContactMethods for a Contact.
     * 
     * @param resolver
     * @param id
     * @return
     */
    public static ContactMethodsCollection getContactMethods (ContentResolver resolver, String id)
    {
        if (id == null)
            return null;
        
        String[] whereArgs = new String[]{id};
        return new ContactMethodsCollection (resolver.query(Contacts.ContactMethods.CONTENT_URI, 
                contactMethodsProjection, 
                "people."+android.provider.BaseColumns._ID+" = ?", 
                whereArgs, Contacts.ContactMethodsColumns.KIND +" DESC"));
    }
    
    public static ContentValues cursorToUserValues(Cursor cursor)
    {
        if (cursor == null)
            return null;
        
        ContentValues values = new ContentValues();
        String val;
        val = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));  
        values.put(android.provider.BaseColumns._ID, val);
        
        val =  cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.DISPLAY_NAME));
        values.put(Contacts.PeopleColumns.DISPLAY_NAME, val);
        
        Integer intVal = new Integer(cursor.getInt(cursor.getColumnIndex(Contacts.PeopleColumns.STARRED)));
        values.put(Contacts.PeopleColumns.STARRED, intVal);
        
        val = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.NOTES));
        values.put(Contacts.PeopleColumns.NOTES, val);
        return values;
    }
}
