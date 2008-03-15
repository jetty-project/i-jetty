package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.util.IO;
import org.mortbay.util.URIUtil;

import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.util.Log;


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
        String pathInfo = request.getPathInfo();
        String servletPath=request.getServletPath();
        String pathInContext=URIUtil.addPaths(servletPath,pathInfo);
        Log.i("Jetty", "pathinfo="+pathInfo);
        
        if (pathInfo==null)
        {
            //redirect any requests for /app/contacts to be /app/contacts/
            RequestDispatcher dispatcher = request.getRequestDispatcher(pathInContext+"/");
            dispatcher.forward(request, response);
        }
        else if ("/".equals(pathInfo.trim()))
        {
            PrintWriter writer = response.getWriter();
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            doHeader(writer, request, response);
            doMenuBar(writer, request, response);
            doContent(writer, request, response);
            doFooter (writer, request, response);
        }
        else
        {
            String who=null;
            String what=null;
            
            StringTokenizer strtok = new StringTokenizer(pathInfo, "/");
            if (strtok.hasMoreElements())          
                who = strtok.nextToken();
            
            if (strtok.hasMoreElements())
                what = strtok.nextToken();

            if (what==null||what.trim().equals(""))
            {
                //try an action instead
                String call = request.getParameter("call");
                if (call!=null)
                {
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    doHeader(writer, request, response);
                    writer.println("<h2>Sorry, phone calls are not available at this time.</h2>");
                    doFooter (writer, request, response);
                }
            }
            else
            {
                if (what.trim().equals("photo"))
                { 
                    String[] projection = new String[] { android.provider.Contacts.PeopleColumns.PHOTO};
                    String where = "people."+android.provider.BaseColumns._ID+" = ?";
                    Cursor cursor = getContentResolver().query(Contacts.People.CONTENT_URI, projection, where, new String[]{who}, null);  
                    if (!cursor.first())
                    { 
                        response.sendError(javax.servlet.http.HttpServletResponse.SC_NO_CONTENT);
                        return;
                    }
                    int i = cursor.getColumnIndex(android.provider.Contacts.PeopleColumns.PHOTO);
                    if (i<0)
                    {
                        response.sendError(javax.servlet.http.HttpServletResponse.SC_NO_CONTENT);
                        return;  
                    }

                    String pic = cursor.getString(i);
                    //handle streaming image
                    if (!pic.contains(":"))
                        pic = "file://"+pic;
                    getServletContext().log("Url for photo="+pic);
                    InputStream is = getContentResolver().openInputStream(Uri.parse(pic));
                    OutputStream os = response.getOutputStream();
                    IO.copy(is,os);
                }
            }
        }
    }

    

    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        if (request.getQueryString()==null)
        {
            String[] projection = new String[] {
                    android.provider.BaseColumns._ID,
                    android.provider.Contacts.PeopleColumns.TITLE,
                    android.provider.Contacts.PeopleColumns.NAME,
                    android.provider.Contacts.PeopleColumns.COMPANY,
                    android.provider.Contacts.PeopleColumns.NOTES,
                    android.provider.Contacts.PhonesColumns.NUMBER,
                    android.provider.Contacts.PhonesColumns.TYPE,
                    android.provider.Contacts.PeopleColumns.PHOTO};
                   // android.provider.Contacts.ContactMethodsColumns.DATA,
                   // android.provider.Contacts.ContactMethodsColumns.KIND};
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
                String id = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));
                
                for (int i=0;i<colNames.length;i++)
                {
                    writer.println("<td class=\""+style+"\">");
                    String strVal = cursor.getString(i);
                    
                    if (colNames[i].equals(Contacts.PhonesColumns.NUMBER))
                    {
                            writer.println((strVal==null?"&nbsp;":"<a href=\"/app/contacts/"+id+"?call="+strVal+"\">"+strVal+"</a>"));
                    }
                    else if (colNames[i].equals(Contacts.PeopleColumns.PHOTO))
                    {
                            writer.println(strVal==null?"&nbsp;":"<img src=\"/app/contacts/"+id+"/photo\""+"/>");
                    }
                    else if (colNames[i].equals(Contacts.PhonesColumns.TYPE))
                    {
                        String phoneType=(String)_phoneTypes.get(Integer.valueOf(cursor.getInt(i)));
                        writer.println((phoneType==null?"":phoneType));
                    }
                    else
                    {
                        writer.println((strVal==null?"&nbsp;":strVal));
                    }
                    writer.println("</td>");
                }
                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");
        }
    }
}
