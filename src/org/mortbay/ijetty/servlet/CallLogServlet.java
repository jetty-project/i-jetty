package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.database.Cursor;
import android.provider.CallLog;

public class CallLogServlet extends InfoServlet
{

    @Override
    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        String[] projection = new String[] 
                                         {
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.NEW,
                CallLog.Calls.NUMBER,
                CallLog.Calls.NUMBER_TYPE,
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
                writer.println("<th>"+colNames[i]+"</th>");
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
