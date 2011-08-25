//========================================================================
//$Id$
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.ijetty.console;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.eclipse.jetty.util.IO;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.util.Log;

public class Contact
{

    
 
    private static final String TAG = "IJetty.Cnsl";

    public final static String __DEFAULT_SORT_ORDER = android.provider.BaseColumns._ID+" ASC";
    public static final String[] baseProjection = new String[]
                                                             { 
                                                              android.provider.BaseColumns._ID, 
                                                              android.provider.Contacts.PeopleColumns.DISPLAY_NAME, 
                                                              android.provider.Contacts.PeopleColumns.NOTES,
                                                              android.provider.Contacts.PeopleColumns.STARRED, 
                                                              android.provider.Contacts.PeopleColumns.SEND_TO_VOICEMAIL,
                                                              android.provider.Contacts.PeopleColumns.CUSTOM_RINGTONE 
                                                             };

    
    /**
     * ContactCollection
     *
     * Inner class wrapping a Cursor over Contacts
     */
    public static class ContactCollection extends DatabaseCollection
    {
        
        public ContactCollection(Cursor cursor)
        {
            super(cursor);
        }
        
        public ContactCollection(Cursor cursor, int startPosition, int limit)
        {
            super(cursor, startPosition, limit);
        }

        @Override
        public ContentValues cursorToValues(Cursor cursor)
        {
            return cursorToUserValues(cursor);
        }

    }

    
    
    /**
     * create
     *
     * Create a new Contact.
     *
     * @param resolver
     * @param values
     * @return
     */
    public static String create(ContentResolver resolver, ContentValues values)
    {
        if (resolver == null)
            return null;

        if (values == null)
            return null;
 
        //Uri uri = Contacts.People.createPersonInMyContactsGroup(resolver,values);
        Uri uri = resolver.insert(Contacts.People.CONTENT_URI, values);
        return String.valueOf(ContentUris.parseId(uri));
    }

    public static ContentValues cursorToUserValues(Cursor cursor)
    {
        if (cursor == null)
            return null;

        ContentValues values = new ContentValues();
        String val;
        val = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));
        values.put(android.provider.BaseColumns._ID,val);

        val = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.DISPLAY_NAME));
        values.put(Contacts.PeopleColumns.DISPLAY_NAME,val);

        Integer intVal = new Integer(cursor.getInt(cursor.getColumnIndex(Contacts.PeopleColumns.STARRED)));
        values.put(Contacts.PeopleColumns.STARRED,intVal);

        val = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.NOTES));
        values.put(Contacts.PeopleColumns.NOTES,val);

        intVal = new Integer(cursor.getInt(cursor.getColumnIndex(Contacts.PeopleColumns.SEND_TO_VOICEMAIL)));
        values.put(Contacts.PeopleColumns.SEND_TO_VOICEMAIL,intVal);

        val = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.CUSTOM_RINGTONE));
        values.put(Contacts.PeopleColumns.CUSTOM_RINGTONE,val);
        return values;
    }

    /**
     * delete
     *
     * Delete a Contact.
     *
     * @param resolver
     * @param id
     */
    public static int delete(ContentResolver resolver, String id)
    {
        if (id == null)
        {
            return 0;
        }

        return resolver.delete(Uri.withAppendedPath(Contacts.People.CONTENT_URI,id),null,null);
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
    public static ContentValues get(ContentResolver resolver, String id)
    {
        if (id == null)
            return null;

        String[] whereArgs = new String[]{ id };
        Cursor cursor = null;
        try
        {
            cursor = resolver.query(Contacts.People.CONTENT_URI,baseProjection,
                                    "people." + android.provider.BaseColumns._ID + " = ?",
                                    whereArgs,Contacts.PeopleColumns.NAME + " ASC");
            if (cursor.moveToFirst())
            {
                ContentValues values = cursorToUserValues(cursor);
                return values;
            }
            else
                return null;
        }
        finally
        {
            if (cursor != null)
                cursor.close();
        }

       
    }

    /**
     * getAll
     *
     * Get all Contacts.
     *
     * @param resolver
     * @return
     */
    public static ContactCollection getContacts (ContentResolver resolver)
    {
        return new ContactCollection(resolver.query(Contacts.People.CONTENT_URI,baseProjection,null,null,__DEFAULT_SORT_ORDER));
    }
    
    
    
    public static ContactCollection getContacts (ContentResolver resolver, int pgStart, int limit)
    {
        return new ContactCollection(resolver.query(Contacts.People.CONTENT_URI, baseProjection, null, null, __DEFAULT_SORT_ORDER), pgStart, limit);
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
    public static void save(ContentResolver resolver, ContentValues values, String id)
    {
        if (resolver == null)
        {
            return;
        }
        if (values == null)
        {
            return;
        }
        if (id == null)
        {
            return;
        }
        Uri uri = Uri.withAppendedPath(Contacts.People.CONTENT_URI,id);
        resolver.update(uri,values,null,null);
    }

    public static void savePhoto(ContentResolver resolver, String id, File photo)
    {
        if (resolver == null)
        {
            return;
        }
        if (photo == null)
        {
            return;
        }
        if (id == null)
        {
            return;
        }
        Uri uri = Uri.withAppendedPath(Contacts.People.CONTENT_URI,id);
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IO.copy(new FileInputStream(photo),out);
            Contacts.People.setPhotoData(resolver,uri,out.toByteArray());
        }
        catch (Exception e)
        {
            Log.e(TAG,"Problem converting photo to bytes for " + photo.getAbsolutePath(),e);
        }
    }
}
