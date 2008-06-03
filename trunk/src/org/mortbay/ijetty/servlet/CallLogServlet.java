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

package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.database.Cursor;
import android.provider.CallLog;

public class CallLogServlet extends InfoServlet
{
	public static final String __ACKNOWLEDGED = "Acknowledged";
	public static final String __DURATION = "Duration (secs)";
	public static final String __INCOMING = "incoming";
	public static final String __OUTGOING = "outgoing";
	public static final String __MISSED = "missed";
	public static final String __YES = "yes";
	public static final String __NO = "no";

	public Map _logTypeMap = new HashMap();
	
	public CallLogServlet ()
	{
		_logTypeMap.put(Integer.valueOf(CallLog.Calls.INCOMING_TYPE), __INCOMING);
		_logTypeMap.put(Integer.valueOf(CallLog.Calls.OUTGOING_TYPE), __OUTGOING);
		_logTypeMap.put(Integer.valueOf(CallLog.Calls.MISSED_TYPE), __MISSED);
	}
    @Override
    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        String[] projection = new String[] 
                                         {
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NEW,
                CallLog.Calls.NUMBER,
                CallLog.Calls.NUMBER_TYPE,
                CallLog.Calls.PERSON_ID,
                CallLog.Calls.NAME
                                         };
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, null);
        writer.println("<h1>Call Log</h1>");
        String[] cols = cursor.getColumnNames();
        formatCallLog(cols, cursor, writer);  
    }

    
    private void formatCallLog (String[] colNames, Cursor cursor, PrintWriter writer)
    {
        if (colNames!=null && cursor!=null && writer!=null)
        {
            writer.println("<table>");
            writer.println("<tr>");
            for (int i=0;i<colNames.length;i++)
            {
            	String cname=null;
            	
            	if (colNames[i].equals(CallLog.Calls.PERSON_ID))
            		continue;
            	else if (colNames[i].equals(CallLog.Calls.NEW))
            		cname=__ACKNOWLEDGED;
            	else if (colNames[i].equals(CallLog.Calls.DURATION))
            		cname=__DURATION;
            	else
            		cname=colNames[i];
            	
                writer.println("<th>"+cname+"</th>");
            }
            writer.println("</tr>");
            int row = 0;
            while (cursor.next())
            {  
                String style = getRowStyle(row);
                writer.println("<tr class='"+style+"'>");
                for (int i=0;i<colNames.length;i++)
                {
                    writer.println("<td class=\""+style+"\">");
                    String val=cursor.getString(i);
                    if (colNames[i].equals(CallLog.Calls.DATE))
                        writer.println(android.util.DateFormat.format("yyyy-MM-dd kk:mm", new Date(cursor.getInt(i))));
                    else if (colNames[i].equals(CallLog.Calls.NEW))
                    {
                    	if (cursor.getInt(i)>0)
                    		writer.println(__YES);
                    	else
                    		writer.println(__NO);
                    }
                    else if (colNames[i].equals(CallLog.Calls.TYPE))
                    {
                    	writer.println(_logTypeMap.get(cursor.getInt(i)));
                    }
                    else if (colNames[i].equals(CallLog.Calls.PERSON_ID))
                    {
                    	int nameIndex = cursor.getColumnIndex(CallLog.Calls.NAME);
                    	String name=(nameIndex>=0?cursor.getString(nameIndex):null);
                    	if (val!=null && name!=null)
                    		writer.println(name==null?"&nbsp;":"<a href='/app/contacts/"+val+"'>"+name+"</a>");	
                    	else
                    		writer.println("&nbsp;");
                    }
                    else if (colNames[i].equals(CallLog.Calls.NAME))
                    {
                    	continue;
                    }
                    else
                        writer.println((val==null?"&nbsp;":val));
                    writer.println("</td>");
                }
                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");

        }
    }
}
