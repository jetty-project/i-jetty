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

import android.content.ContentValues;
import android.database.Cursor;



/**
 * DatabaseCollection
 * 
 * Ideally this would use a sqlite select with limit, but
 * unfortunately Android ContentProvider does not support
 * that.
 */
public abstract class DatabaseCollection
{
    protected Cursor cursor;
    protected int startPos = 0;
    protected int limit = -1;
    protected int count = 0;
    
    protected boolean inited = false;

    public DatabaseCollection(Cursor cursor)
    {
        this.cursor = cursor;
    }
    
    /**
     * @param cursor
     * @param startPos number of rows to skip
     * @param limit number of rows to return
     */
    public DatabaseCollection(Cursor cursor, int startPos, int limit)
    {
        this(cursor);
        this.startPos = startPos;
        this.limit = limit;
    }

    public void close()
    {
        if (cursor != null)
            cursor.close();
    }

    public abstract ContentValues cursorToValues(Cursor cursor);

    
    public long getTotal ()
    {
        if (cursor != null)
            return cursor.getCount();
        
        return -1;
    }
    
    public int getStartPos ()
    {
        return this.startPos;
    }
    
    public int getLimit ()
    {
        return this.limit;
    }
    
    public boolean hasNext()
    {
        init();
        return cursor != null && (limit < 0 || (count < limit)) && !cursor.isLast();
    }

    public ContentValues next()
    {
        ContentValues values = null;

        init();
        if ((limit < 0 || (count < limit)) && cursor.moveToNext())
        {
            ++count;
            return cursorToValues(cursor);
        }

        return values;
    }
    
    private void init ()
    {
        if (!inited)
        {
            count = 0;
            if (cursor != null)      
            {
                //we need to skip forward a number of rows
                if (startPos > 0)
                {
                    //skip to just before the desired starting pos if the starting pos isn't
                    //beyond the number of rows
                    if (startPos < cursor.getCount()) {
                        cursor.move(startPos);
                    }
                    else {
                        cursor.moveToLast();
                    }
                }
                else if (startPos < 0) {
                    cursor.moveToLast(); // don't return any rows as we've gone negative
                }
            }
            inited = true;
        }
    }
}
