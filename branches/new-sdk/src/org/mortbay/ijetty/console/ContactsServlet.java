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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.IO;
import org.mortbay.util.URIUtil;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.util.Log;

import org.mortbay.ijetty.console.InfoServlet;

public class ContactsServlet extends InfoServlet
{
        private static final String __WORK = "work";
        private static final String __HOME = "home";
        private static final String __OTHER = "other";
        private static final String __POSTAL = "postal";
        private static final String __MOBILE = "mobile";
        private static final String __PAGER = "pager";
        private static final String __WORK_FAX = "work fax";
        private static final String __HOME_FAX = "home fax";
        private static final String __PRIMARY = "primary";
        private static final String __EMAIL = "email";
        private static final String __JABBER = "jabber";
        private static final String __GEO_LOCATION = "geo location";
        
        
    private Map _phoneTypes = new HashMap();
    private Map _contactEmailTypes = new HashMap();
    private Map _postalTypes = new HashMap();
    

    String[] baseProjection = new String[] {
            android.provider.BaseColumns._ID,
            android.provider.Contacts.PeopleColumns.DISPLAY_NAME,
            /*android.provider.Contacts.PeopleColumns.COMPANY,*/
            android.provider.Contacts.PeopleColumns.NOTES,
            /*android.provider.Contacts.PeopleColumns.PHOTO,*/
            android.provider.Contacts.PeopleColumns.PHOTO_VERSION,
            android.provider.Contacts.PeopleColumns.STARRED
    };
    
    String[] contactMethodsProjection = new String[] {
            android.provider.BaseColumns._ID,
            android.provider.Contacts.ContactMethodsColumns.DATA,
            android.provider.Contacts.ContactMethodsColumns.AUX_DATA,
            android.provider.Contacts.ContactMethodsColumns.KIND,
            android.provider.Contacts.ContactMethodsColumns.TYPE
    };
    
    String[] phonesProjection = new String[] {
            android.provider.BaseColumns._ID,
            android.provider.Contacts.PhonesColumns.LABEL,
            android.provider.Contacts.PhonesColumns.NUMBER,
            android.provider.Contacts.PhonesColumns.NUMBER_KEY,
            android.provider.Contacts.PhonesColumns.TYPE      
    };
    
    
    public class User 
    {
        String title;
        String name;
        String company;
        String notes;
        String photo;
        boolean starred;
        List contactMethods = new ArrayList();
        List phones = new ArrayList();
    }
    
    public ContactsServlet ()
    {
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_MOBILE), __MOBILE);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_HOME), __HOME);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_WORK), __WORK);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_FAX_WORK), __WORK_FAX);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_FAX_HOME), __HOME_FAX);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_PAGER), __PAGER);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_OTHER), __OTHER);
        
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_HOME), __HOME);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_WORK), __WORK);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.ISPRIMARY), __PRIMARY);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_OTHER), __OTHER);
        
        /*_postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_HOME_TYPE), __HOME);
        _postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_OTHER_TYPE), __OTHER);
        _postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_POSTAL_TYPE), __POSTAL);
        _postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_WORK_TYPE), __WORK);*/
    }
    
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String pathInfo = request.getPathInfo();
        String servletPath=request.getServletPath();
        String pathInContext=URIUtil.addPaths(servletPath,pathInfo);
        Log.i("Jetty", "pathinfo="+pathInfo);
        
        if (pathInfo==null)
        {
            //redirect any requests for /console/contacts to be /console/contacts/
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
            doBaseContent(writer, request, response);
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
            
            Log.i("Jetty", "who="+who+" what="+what);

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
                    writer.println("<h2 style='text-align: center;'>Sorry, phone calls are not available at this time.</h2>");
                    doFooter (writer, request, response);
                }
                else
                {
                    //show the details for a particular user
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    doHeader(writer, request, response);
                    doMenuBar(writer, request, response);
                    doUserContent(writer, request, response, who);
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
                        cursor.close();
                        return;
                    }
                    int i = cursor.getColumnIndex(android.provider.Contacts.PeopleColumns.PHOTO);
                    if (i<0)
                    {
                        response.sendError(javax.servlet.http.HttpServletResponse.SC_NO_CONTENT);
                        cursor.close();
                        return;  
                    }

                    String pic = cursor.getString(i);
                    //handle streaming image
                    if (!pic.contains(":"))
                        pic = "file://"+pic;
                    getServletContext().log("Url for photo="+pic);
                    InputStream is = getContentResolver().openInputStream(Uri.parse(pic));
                    response.setContentType(getServletContext().getMimeType(pic));
                    OutputStream os = response.getOutputStream();
                    IO.copy(is,os);
                    cursor.close();
                }
            }
        }
    }

    protected void doContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doBaseContent(writer, request, response);
    }

    protected void doBaseContent(PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<h1>Contact List</h1><div id='content'>");
        Cursor cursor = getContentResolver().query(Contacts.People.CONTENT_URI, baseProjection, null, null, null);  
        if (cursor!=null)
        {
            formatUserDetails (cursor, writer);
            cursor.close();
        }
        writer.println("</div>");
    }
    
    protected void doUserContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {
        String[] whereArgs = new String[]{who};
        
        //query for the user's standard details
        Cursor cursor = getContentResolver().query(Contacts.People.CONTENT_URI, baseProjection, "people."+android.provider.BaseColumns._ID+" = ?", whereArgs, Contacts.PeopleColumns.NAME+" ASC");
        formatSummaryUserDetails (cursor, writer);
        cursor.close();
        //query for all phone details
        cursor = getContentResolver().query(Contacts.Phones.CONTENT_URI, phonesProjection, "people."+android.provider.BaseColumns._ID+" = ?", whereArgs, Contacts.PhonesColumns.TYPE+" ASC");

        formatPhones (who, cursor, writer);
        cursor.close();
        
        //query for all contact details
        cursor = getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, contactMethodsProjection, "people."+android.provider.BaseColumns._ID+" = ?", whereArgs, Contacts.ContactMethodsColumns.KIND +" DESC");
        formatContactMethods (who, cursor, writer);
        cursor.close(); 
        
        //TODO - implement buttons
        writer.println("<br /><button id='edit'>Edit</button>&nbsp;<button id='del'>Delete</button>");
    }


    private void formatUserDetails (Cursor cursor, PrintWriter writer)
    {
        if (cursor!=null && writer!=null)
        {
            writer.println("<table id='user' style='border: 0px none;'>");
            int row = 0;
            while (cursor.moveToNext())
            {  
                String style = getRowStyle(row);
                
                writer.println("<tr class='"+style+"'>");
                String id = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));  
                String name =  cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.NAME));
                String title = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.TITLE));
                String company = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.COMPANY));
                String notes = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.NOTES));
                String photo = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.PHOTO));
                boolean starred = (cursor.getInt(cursor.getColumnIndex(Contacts.PeopleColumns.STARRED)) >0?true:false);
                printCell (writer, (starred?"<span class='big'>*</span>":"&nbsp;"), style);
                printCell(writer, (photo==null?"&nbsp;":"<a href='/console/contacts/"+id+"/'><img src=\"/console/contacts/"+id+"/photo\""+"/></a>"), style);
                if (title!=null)
                        printCell(writer, "["+title+"]&nbsp;<a href=\"/console/contacts/"+id+"/\">"+name+"</a>", style);
                else
                        printCell(writer, "<a href=\"/console/contacts/"+id+"\">"+name+"</a>", style);
                printCell(writer, (company==null?"":company), style);
               
                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");
            
            if (row==0)
            {
                writer.println("<h2 style='text-align: center;'>Sorry, you haven't added any contacts to your phone!</h2>");
            }
        }
    }
    
    private void formatSummaryUserDetails (Cursor cursor, PrintWriter writer)
    {
         if (cursor!=null && writer!=null)
         {
           if (cursor.moveToFirst())
           {
               String id = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));  
               String name =  cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.NAME));
               /*String title = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.TITLE));*/
               /*String company = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.COMPANY));*/
               String notes = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.NOTES));
               /*String photo = cursor.getString(cursor.getColumnIndex(Contacts.PeopleColumns.PHOTO));*/
               boolean starred = (cursor.getInt(cursor.getColumnIndex(Contacts.PeopleColumns.STARRED)) >0?true:false);
               writer.println("<h1>"+(starred?"<span class='big'>*</span>&nbsp;":"")+/*(title==null?"":title+"&nbsp;")+*/(name==null?"Unknown":name)+"</h1><div id='content'>");
               /*if (photo!=null)
                   writer.println("<h2>Photo</h2><a href='/console/contacts/"+id+"/photo'><img src=\"/console/contacts/"+id+"/photo\""+"/></a>");*/
               /*if (company!=null)
                   writer.println("<p>Company: "+company+"</h3></p>");*/
               writer.println("<h2>Notes</h2>");
               writer.println("<table id='notes' style='border: 0px none;'>");
               writer.println("<tr>"); 
               writer.println("<td>"); 
               if (notes!=null)
                   writer.println(notes);
               else
                   writer.println("&nbsp;");
               writer.println("</td>");
               writer.println("</tr>");
               writer.println("</table>");
           }
         }
    }
    
    private void formatPhones (String who, Cursor cursor, PrintWriter writer)
    {
        writer.println("<h2>Phone Numbers</h2>");
        writer.println("<table id='phones' style='border: 0px none;'>");
        int row = 0;
        while (cursor.moveToNext())
        {  
                String style = getRowStyle(row);
                writer.println("<tr class='"+style+"'>");
                String label = cursor.getString(cursor.getColumnIndex(Contacts.PhonesColumns.LABEL));
                String number = cursor.getString(cursor.getColumnIndex(Contacts.PhonesColumns.NUMBER));
                int type = cursor.getInt(cursor.getColumnIndex(Contacts.PhonesColumns.TYPE));
                String phoneType=(String)_phoneTypes.get(Integer.valueOf(type));
                printCell(writer, (label==null?"":"<span class='label'>"+label+"</span>"), style);
                String encodedNumber = number;
                try
                {
                encodedNumber = URLEncoder.encode(number, "UTF-8");             
                }
                catch (Exception e)
                {
                        Log.w("Jetty", "Encoding telephone number failed");
                }
                printCell(writer, (number==null?"&nbsp;":"<a href=\"/console/contacts/"+who+"?call="+encodedNumber+"\">"+number+"</a>&nbsp;<span class='qualifier'>["+phoneType+"]</span>"), style);
                writer.println("</tr>");
                row++;
        }
        writer.println("</table>");
    }
    
    private void formatContactMethods (String who, Cursor cursor, PrintWriter writer)
    {
        writer.println("<h2>Addresses</h2>");
        writer.println("<table id='addresses' style='border: 0px none;'>");
        int row = 0;
        while (cursor.moveToNext())
        { 
                String style = getRowStyle(row);
            writer.println("<tr class='"+style+"'>");
            String data = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.DATA));
            String auxData = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethodsColumns.AUX_DATA));
            int kind = -99;
            int type = -99;
            if (data!=null||auxData!=null)
            {
                kind = cursor.getInt(cursor.getColumnIndex(Contacts.ContactMethodsColumns.KIND));
                type = cursor.getInt(cursor.getColumnIndex(Contacts.ContactMethodsColumns.TYPE));
            }
            switch (kind)
            {
                case Contacts.ContactMethodsColumns.EMAIL_KIND:
                {
                        printCell(writer, "<span class='label'>"+__EMAIL+":</span>", style);
                        if (type==Contacts.ContactMethodsColumns.EMAIL_KIND_PRIMARY_TYPE)
                                printCell(writer, "<a href=\"mailto:"+data+"\">"+data+"</a>", style);
                        else
                        {
                                String typeStr = (String)_contactEmailTypes.get(Integer.valueOf(type));
                                printCell(writer, "<a href=\"mailto:"+data+"\">"+"("+typeStr+")"+data+"</a>", style);
                        }
                        printCell(writer, "", style);
                        break;
                }
                case Contacts.ContactMethodsColumns.JABBER_IM_KIND:
                {
                        printCell(writer, "<span class='label'>"+__JABBER+":</span>", style);
                        printCell(writer, data, style);
                        printCell(writer, "", style);
                        break;
                }
                case Contacts.ContactMethodsColumns.POSTAL_KIND:
                {
                        printCell(writer, "<span class='label'>"+__POSTAL+":</span>", style);
                        String typeStr = (String)_contactEmailTypes.get(Integer.valueOf(type));
                                printCell(writer, "<span class='qualifier'>["+typeStr+"]</span>&nbsp;"+data, style);
                                printCell(writer, "", style);
                        break;
                }
                case Contacts.ContactMethodsColumns.LOCATION_KIND:
                {
                        printCell(writer, "<span class='label'>"+__GEO_LOCATION+":</span>", style);
                        String typeStr = (String)_contactEmailTypes.get(Integer.valueOf(type));
                        printCell(writer, "<span class='qualifier'>["+typeStr+"]</span>&nbsp;"+data, style);
                        printCell(writer, auxData, style);
                        break;
                }
                default:
                {
                        if (data!=null)
                                printCell(writer, data, style);
                        if (auxData!=null)
                                printCell(writer, data, style);
                        break;
                }
            }
            writer.println("</tr>");
            row++;
        }
        
        writer.println("</table>");
    }  
    
    private void printCell (PrintWriter writer, String cellContent, String cellStyle)
    {                
        writer.println("<td" + cellStyle + ">");
        writer.println(cellContent);
        writer.println("</td>");
    }
    
    private void printCellHeader (PrintWriter writer, String cellContent, String cellStyle)
    {
        writer.println("<th>"+cellContent+"</th>");
    }
}
