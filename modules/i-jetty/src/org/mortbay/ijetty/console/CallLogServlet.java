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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.database.Cursor;
import android.provider.CallLog;

import org.mortbay.ijetty.console.InfoServlet;

public class CallLogServlet extends InfoServlet
{
        public static final String __ACKNOWLEDGED = "Acknowledged";
        public static final String __DURATION = "Duration (secs)";
        public static final String __INCOMING = "incoming";
        public static final String __OUTGOING = "outgoing";
        public static final String __MISSED = "missed";
        public static final String __YES = "yes";
        public static final String __NO = "no";
        
        public static final String __CSV_DELIM = ",";

        public Map _logTypeMap = new HashMap();
        
        public CallLogServlet ()
        {
                _logTypeMap.put(Integer.valueOf(CallLog.Calls.INCOMING_TYPE), __INCOMING);
                _logTypeMap.put(Integer.valueOf(CallLog.Calls.OUTGOING_TYPE), __OUTGOING);
                _logTypeMap.put(Integer.valueOf(CallLog.Calls.MISSED_TYPE), __MISSED);
        }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        PrintWriter writer = response.getWriter();
        
        String csv = request.getParameter("csv");
        if (csv != null && Integer.parseInt(csv.trim()) >= 1)
        {
            response.setContentType("text/csv");
            response.setStatus(HttpServletResponse.SC_OK);
            doContent(writer, request, response);
        }
        else
        {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            doHeader (writer, request, response);
            doMenuBar(writer, request, response);
            doContent(writer, request, response);
            doFooter (writer, request, response);
        }
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
                CallLog.Calls.CACHED_NUMBER_TYPE,
                CallLog.Calls.CACHED_NAME
                                         };
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, null);
        String[] cols = cursor.getColumnNames();
        
        String csv = request.getParameter("csv");
        if (csv != null && Integer.parseInt(csv.trim()) >= 1)
        {
            formatCSV(cols, cursor, writer);
        }
        else
        {
            writer.println("<h1>Call Log</h1><div id='content'>");
            formatCallLog(cols, cursor, writer);  
            writer.println("<p><small><a href='?csv=1'>Download as CSV</a></small></p>");
            writer.println("</div>");
        }
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
                
                if (colNames[i].equals(CallLog.Calls.NEW))
                        cname=__ACKNOWLEDGED;
                else if (colNames[i].equals(CallLog.Calls.DURATION))
                        cname=__DURATION;
                else
                        cname=colNames[i];
            
                writer.println("<th>"+cname+"</th>");
            }
            writer.println("</tr>");
            int row = 0;
            while (cursor.moveToNext())
            {  
                String style = getRowStyle(row);
                writer.println("<tr>");
                for (int i=0;i<colNames.length;i++)
                {
                    writer.println("<td"+style+">");
                    String val=cursor.getString(i);
                    if (colNames[i].equals(CallLog.Calls.DATE))
                    {
                        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date (cursor.getInt(i)*1000));
                        writer.println((new Date(cursor.getInt(i))).toString());
                    }
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
                    else if (colNames[i].equals(CallLog.Calls.CACHED_NAME))
                    {
                        String name = cursor.getString(i);
                        if (name != null)
                                writer.println(name==null?"&nbsp;":"<a href='/console/contacts/"+row+"'>"+name+"</a>");     
                        else
                                writer.println("&nbsp;");
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
    
    private void formatCSV (String[] colNames, Cursor cursor, PrintWriter writer)
    {
        if (colNames!=null && cursor!=null && writer!=null)
        {            
            for (int i=0;i<colNames.length;i++)
            {
                String cname=colNames[i];
                
                // Since we provide the contact's *name* and their ID,
                // we always do both at the same time, so we should do the
                // same here.
                if (colNames[i].equals(CallLog.Calls.CACHED_NAME))
                    cname += __CSV_DELIM + "contactid";
                
                printCSV(i, colNames.length, writer, cname);
            }
            
            int row = 0;
            while (cursor.moveToNext())
            {  
                String style = getRowStyle(row);
                for (int i=0;i<colNames.length;i++)
                {
                    String val=cursor.getString(i);
                    if (colNames[i].equals(CallLog.Calls.DATE))
                    {
                        printCSV(i, colNames.length, writer, val);
                    }
                    else if (colNames[i].equals(CallLog.Calls.NEW))
                    {
                        if (cursor.getInt(i)>0)
                                printCSV(i, colNames.length, writer, __YES);
                        else
                                printCSV(i, colNames.length, writer, __NO);
                    }
                    else if (colNames[i].equals(CallLog.Calls.TYPE))
                    {
                        printCSV(i, colNames.length, writer, _logTypeMap.get(cursor.getInt(i)).toString());
                    }
                    else if (colNames[i].equals(CallLog.Calls.CACHED_NAME))
                    {
                        String name = cursor.getString(i);
                        if (name != null)
                            printCSV(i, colNames.length, writer, "\"" + name + "\"" + __CSV_DELIM +  row);
                        else
                            // Empty pair of commas (no data)
                            printCSV(i, colNames.length, writer, __CSV_DELIM);
                    }
                    else
                        printCSV(i, colNames.length, writer, (val==null?"":val));
                }
                ++row;
            }
        }
    }
    
    private void printCSV(int col, int length, PrintWriter writer, String value)
    {
        if (col != (length - 1))
        {
            writer.print(value + __CSV_DELIM);
        }
        else
        {
            writer.println(value);
        }
    }
}
