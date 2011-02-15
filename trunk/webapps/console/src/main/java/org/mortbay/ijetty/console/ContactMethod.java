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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;

public class ContactMethod
{
    public static class ContactMethodsCollection extends DatabaseCollection
    {
        public ContactMethodsCollection(Cursor cursor)
        {
            super(cursor);
        }

        @Override
        public ContentValues cursorToValues(Cursor cursor)
        {
            if (cursor == null)
            {
                return null;
            }

            ContentValues values = new ContentValues();
            String val;
            val = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));
            values.put(android.provider.BaseColumns._ID,val);
            val = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.DATA));
            values.put(Contacts.ContactMethodsColumns.DATA,val);
            val = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.AUX_DATA));
            values.put(Contacts.ContactMethodsColumns.AUX_DATA,val);
            val = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.LABEL));
            values.put(Contacts.ContactMethodsColumns.LABEL,val);
            Integer intVal = new Integer(cursor.getInt(cursor.getColumnIndex(Contacts.ContactMethodsColumns.ISPRIMARY)));
            values.put(Contacts.ContactMethodsColumns.ISPRIMARY,intVal);
            intVal = new Integer(cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.KIND)));
            values.put(Contacts.ContactMethodsColumns.KIND,intVal);
            intVal = new Integer(cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.TYPE)));
            values.put(Contacts.ContactMethodsColumns.TYPE,intVal);
            return values;
        }
    }

    static final String[] contactMethodsProjection = new String[]
    { android.provider.BaseColumns._ID, android.provider.Contacts.ContactMethodsColumns.DATA, android.provider.Contacts.ContactMethodsColumns.AUX_DATA,
            android.provider.Contacts.ContactMethodsColumns.KIND, android.provider.Contacts.ContactMethodsColumns.LABEL,
            android.provider.Contacts.ContactMethodsColumns.TYPE, android.provider.Contacts.ContactMethodsColumns.ISPRIMARY };

    public static void addContactMethod(ContentResolver resolver, ContentValues contactMethod, String userId)
    {
        Uri peopleUri = Uri.withAppendedPath(Contacts.People.CONTENT_URI,userId);
        resolver.insert(Uri.withAppendedPath(peopleUri,Contacts.People.ContactMethods.CONTENT_DIRECTORY),contactMethod);
    }

    public static void deleteContactMethod(ContentResolver resolver, String id, String userId)
    {
        resolver.delete(Uri.withAppendedPath(Contacts.ContactMethods.CONTENT_URI,id),null,null);
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
    public static ContactMethodsCollection getContactMethods(ContentResolver resolver, String userId)
    {
        if (userId == null)
        {
            return null;
        }

        String[] whereArgs = new String[]
        { userId };
        StringBuilder where = new StringBuilder();
       
        where.append(Contacts.ContactMethods.PERSON_ID);
        where.append(" = ?");
        return new ContactMethodsCollection(resolver.query(Contacts.ContactMethods.CONTENT_URI,contactMethodsProjection,where.toString(),whereArgs,
                Contacts.ContactMethodsColumns.KIND + " DESC"));
    }

    public static void saveContactMethod(ContentResolver resolver, ContentValues contactMethod, String id, String userId)
    {
        Uri uri = Uri.withAppendedPath(Contacts.ContactMethods.CONTENT_URI,id);
        resolver.update(uri,contactMethod,null,null);
    }
}
