package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.util.IO;

import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;

public class ContactsServlet extends InfoServlet
{
    private Map _phoneTypes = new HashMap();
    
    public ContactsServlet ()
    {
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.MOBILE_TYPE), "mobile");
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.HOME_TYPE), "home");
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.WORK_TYPE), "work");
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.WORK_FAX_TYPE), "work fax");
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.HOME_FAX_TYPE), "home fax");
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.PAGER_TYPE), "pager");
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.PAGER_TYPE), "other");
    }
    
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        base_request.setHandled(true);
        
        String pic = request.getParameter("pic");
        if (pic!=null)
        {
            //handle streaming image
            InputStream is = getContentResolver().openInputStream(Uri.parse(pic));
            OutputStream os = response.getOutputStream();
            IO.copy(is,os);
        }
        else 
        {  
            PrintWriter writer = response.getWriter();
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            doHeader(writer, request, response);
            doContent(writer, request, response);
            doFooter (writer, request, response);
        }
    }

    

    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        if (request.getQueryString()==null)
        {
            String[] projection = new String[] {
                    android.provider.BaseColumns._ID,
                    android.provider.Contacts.PeopleColumns.NAME,
                    android.provider.Contacts.PhonesColumns.NUMBER,
                    android.provider.Contacts.PhonesColumns.TYPE,
                    android.provider.Contacts.PeopleColumns.PHOTO};
            Cursor cursor = getContentResolver().query(Contacts.People.CONTENT_URI, projection, null, null, null);  
            if (cursor!=null)
            {
                String[] cols = cursor.getColumnNames();
                formatContacts (cols, cursor, writer);
            } 
        }
    }


    private void formatContacts (String[] colNames, Cursor cursor, PrintWriter writer)
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
                
                writer.println("<tr>");
                for (int i=0;i<colNames.length;i++)
                {
                    writer.println("<td class=\""+style+"\">");
                    String strVal = cursor.getString(i);
                    if (colNames[i].equals(Contacts.PhonesColumns.NUMBER))
                    {
                        if (strVal!=null)
                            writer.println("<a href=\"/app/contacts/?call=\""+strVal+"\">"+strVal+"</a>");
                        else
                            writer.println("&nbsp;");
                    }
                    else if (colNames[i].equals(Contacts.PeopleColumns.PHOTO))
                    {
                        if (strVal!=null)
                            writer.println("<a href=\"/app/contacts/?pic=\""+cursor.getString(i)+"\">"+"image"+"</a>");
                        else
                            writer.println("&nbsp;");
                    }
                    else if (colNames[i].equals(Contacts.PhonesColumns.TYPE))
                    {
                        String phoneType=(String)_phoneTypes.get(Integer.valueOf(cursor.getInt(i)));
                        writer.println((phoneType==null?"":phoneType));
                    }
                    else
                        writer.println(strVal);
                    writer.println("</td>");
                }
                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");
        }
    }
}
