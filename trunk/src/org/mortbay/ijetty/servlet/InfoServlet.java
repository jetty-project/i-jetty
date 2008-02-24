package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.Contacts;
import android.provider.Settings;

public class InfoServlet extends HttpServlet
{
    private ContentResolver _resolver;

    public void setContentResolver (ContentResolver resolver)
    {
        _resolver=resolver;
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        base_request.setHandled(true);

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();

        writer.println("<html>");
        writer.println("<head><META http-equiv=\"Pragma\" content=\"no-cache\"> <META http-equiv=\"Cache-Control\" content=\"no-cache,no-store\">");
        writer.println(" <link rel=\"stylesheet\" type=\"text/css\" href=\"/?css\" /></head>");
        Map params = request.getParameterMap();
        boolean wantContacts = params.get("contacts")!=null;
        boolean wantSystemSettings = params.get("system")!=null;
        boolean wantCallLog = params.get("calls")!=null;
        boolean wantCss = params.get("css")!=null;
        if (params.isEmpty())
        {
            writer.println("<h1>Options</h1>");
            writer.println("<ul>");
            writer.println("<li><a href='/?contacts'>Contacts</a></li>");
            writer.println("<li><a href='/?system'>System Settings</a></li>");
            writer.println("<li><a href='/?calls'>Call Log</a></li>");
            writer.println("</ul>");
        }
        else if (wantContacts)
            doContacts(_resolver, writer);
        else if (wantSystemSettings)
            doSystemSettings (_resolver, writer);
        else if (wantCallLog)
            doCallLog (_resolver, writer);
        else if (wantCss)
            doCss (writer);
        
        writer.println("</html>"); 
    }

    public void doContacts (ContentResolver resolver, PrintWriter writer)
    {
        String[] projection = new String[] {
                android.provider.BaseColumns._ID,
                android.provider.Contacts.PeopleColumns.NAME,
                android.provider.Contacts.PhonesColumns.NUMBER,
                android.provider.Contacts.PeopleColumns.PHOTO};
        Cursor cursor = resolver.query(Contacts.People.CONTENT_URI, projection, null, null, null);  
        if (cursor!=null)
        {
            String[] cols = cursor.getColumnNames();
            formatTable (cols, cursor, writer);
        }
    }

    public void doCallLog (ContentResolver resolver, PrintWriter writer)
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
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, null);
        writer.println("<h1>Call Log</h1>");
        String[] cols = cursor.getColumnNames();
        formatTable(cols, cursor, writer);  
    }

    public void doSystemSettings (ContentResolver resolver, PrintWriter writer)
    {  
        writer.println("<h1>System Settings</h1>");
        Cursor cursor = resolver.query(Settings.System.CONTENT_URI, null, null, null, null);
        String[] cols = cursor.getColumnNames();
        formatTable(cols, cursor, writer);
    }

    public void doCss (PrintWriter writer)
    {
        writer.println("table {border-width:3px;border-style:groove;border-color:#aaaaaa;}");
        writer.println("th {font-variant:small-caps;text-align:left;}");
        writer.println(".odd {background:#efefef;border:solid #aaaaaa 1px;}");
        writer.println(".even {background:#ffffff;border:solid #aaaaaa 1px;}");
    }
    
    
    private void formatTable (String[] colNames, Cursor cursor, PrintWriter writer)
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
                String style = "";
                if (row%2==0)
                    style = "even";
                else
                    style = "odd";

                writer.println("<tr>");
                for (int i=0;i<colNames.length;i++)
                    writer.println("<td class=\""+style+"\">"+cursor.getString(i)+"</td>");

                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");

        }
    }
}
