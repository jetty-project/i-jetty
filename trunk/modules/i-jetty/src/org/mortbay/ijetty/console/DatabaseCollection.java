package org.mortbay.ijetty.console;

import android.content.ContentValues;
import android.database.Cursor;

public abstract class DatabaseCollection
{
    protected Cursor cursor;

    public abstract ContentValues cursorToValues (Cursor cursor);

    public DatabaseCollection (Cursor cursor)
    {
        this.cursor = cursor;    
    }

    public ContentValues next ()
    {
        ContentValues values = null;

        if (cursor.moveToNext())
        {
            return cursorToValues(cursor);
        }

        return values;
    }

    public void close ()
    {
        cursor.close();
    } 

}
