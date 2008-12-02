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
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.mortbay.ijetty.console.User.PhoneCollection;
import org.mortbay.util.IO;
import org.mortbay.util.URIUtil;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.util.Log;

public class ContactsServlet extends InfoServlet
{
    private static final int __ACTION_NONE = -1;
    private static final int __ACTION_CALL = 0;
    private static final int __ACTION_EDIT = 1;
    private static final int __ACTION_ADD = 2;
    private static final int __ACTION_DEL = 3;
    private static final int __ACTION_SAVE = 4;


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
    private static final String __CUSTOM = "custom";
    private static final String __IM = "IM";
    private static final String __GEO_LOCATION = "geo location";
    
    // FIXME: Use a local copy when finished testing. :)
    private static final String[] __JAVASCRIPT = new String[] { "http://code.jquery.com/jquery-latest.min.js", "http://tablesorter.com/jquery.tablesorter.min.js", "/console/contacts.js" };


    private Map _phoneTypes = new HashMap();
    private Map _contactEmailTypes = new HashMap();
    private Map _postalTypes = new HashMap();
    

    
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
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_OTHER), __OTHER);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_CUSTOM), __CUSTOM);
  
        //_contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.ISPRIMARY), __PRIMARY);
        
        /*_postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_HOME_TYPE), __HOME);
        _postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_OTHER_TYPE), __OTHER);
        _postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_POSTAL_TYPE), __POSTAL);
        _postalTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.POSTAL_KIND_WORK_TYPE), __WORK);*/
    }
    
    
    protected void doPost (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
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
        else
        {
            String who=null;
            String what=null;
            boolean json=false;
            
            StringTokenizer strtok = new StringTokenizer(pathInfo, "/");
            if (strtok.hasMoreElements())          
                who = strtok.nextToken();
            
            if (strtok.hasMoreElements())
                what = strtok.nextToken();
            
            String str = request.getParameter("action");
            int action = (str==null? __ACTION_NONE : Integer.parseInt(str.trim()));
            
            str = request.getParameter("json");
            json = (str==null ? false : Integer.parseInt(str.trim()) == 1);
            
            Log.i("Jetty", "who="+who+" what="+what+" action="+action);
            
            switch (action)
            {
                case __ACTION_NONE:
                {
                    if ((what == null) && (who != null))
                    {
                        //default: nothing specific to do, show user info
                        PrintWriter writer = response.getWriter();
                        if (json)
                        {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_OK);
                            doUserContentJSON(writer, request, response, who);
                        }
                        else
                        {
                            response.setContentType("text/html");
                            response.setStatus(HttpServletResponse.SC_OK);
                            doHeader(writer, request, response);
                            doMenuBar(writer, request, response);
                            doUserContent(writer, request, response, who);
                            doFooter (writer, request, response);
                        }
                    }
                    else if ((what == null) && (who == null))
                    {
                        //no specific user, show all of them
                        PrintWriter writer = response.getWriter();
                        response.setContentType("text/html");
                        response.setStatus(HttpServletResponse.SC_OK);
                        doHeader(writer, request, response, __JAVASCRIPT);
                        doMenuBar(writer, request, response);
                        doBaseContent(writer, request, response);
                        doFooter (writer, request, response);
                    }
                    else if ("photo".equals(what.trim()))
                    {
                        //ask for the photo
                        Uri personUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI,Long.valueOf(who.trim()).longValue());
                        InputStream is = Contacts.People.openContactPhotoInputStream(getContentResolver(), personUri);

                        
                        if (is == null)
                        {
                            response.setContentType("application/octet-stream"); 
                            OutputStream os = response.getOutputStream();
                            is = getServletContext().getResourceAsStream("/android.jpg");
                            IO.copy(is,os);
                            //response.sendError(HttpStatus.SC_NOT_FOUND);
                        }
                        else
                        {
                            response.setContentType("application/octet-stream");
                            OutputStream os = response.getOutputStream();
                            IO.copy(is,os);
                        }
                    }
                    break;
                }
                case __ACTION_CALL:
                {
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    doHeader(writer, request, response);
                    writer.println("<h2 style='text-align: center;'>Sorry, phone calls are not available at this time.</h2>");
                    doFooter (writer, request, response);
                
                    break;
                }
                case __ACTION_ADD:
                {
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    doHeader(writer, request, response);
                    doMenuBar(writer, request, response);
                    doEditUser(writer, request, response, null);
                    doFooter (writer, request, response);
                
                    break;
                }
                case __ACTION_EDIT:
                {
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    doHeader(writer, request, response);
                    doMenuBar(writer, request, response);
                    doEditUser(writer, request, response, who);
                    doFooter (writer, request, response);
                    break;
                }
                case __ACTION_SAVE:
                {
                    PrintWriter writer = response.getWriter();
                    
                    if (json)
                    {
                        response.setContentType("application/json");
                        doSaveUserJSON(writer, request, response, request.getParameter("id"));
                    }
                    else
                    {
                        response.setContentType("text/html");
                        response.setStatus(HttpServletResponse.SC_OK);
                        doHeader(writer, request, response);
                        doMenuBar(writer, request, response);
                        doSaveUser(writer, request, response, request.getParameter("id"));
                        doFooter (writer, request, response);
                    }
                    break;
                }
                case __ACTION_DEL:
                {
                    PrintWriter writer = response.getWriter();
                    
                    if (json)
                    {
                        response.setContentType("application/json");
                        doDeleteUserJSON(writer, request, response, who);
                    } 
                    else
                    {
                        response.setContentType("text/html");
                        response.setStatus(HttpServletResponse.SC_OK);
                        doHeader(writer, request, response);
                        doMenuBar(writer, request, response);
                        doDeleteUser(writer, request, response, who /*request.getParameter("id")*/);
                        doFooter (writer, request, response);
                    }
                    break;
                }
                default:
                {
                    PrintWriter writer = response.getWriter();
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    doHeader(writer, request, response);
                    doMenuBar(writer, request, response);
                    doBaseContent(writer, request, response);
                    doFooter (writer, request, response);
                }
            }
        }
    }
    
    
    protected void doDeleteUserJSON (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id)
    throws ServletException, IOException
    {
        try
        {
            User.delete(getContentResolver(), id);
        }
        catch (Exception e)
        {
            // TODO: Better error catching - ie. check for invalid user and just failure at remove
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
        writer.println("{ \"status\": \"OK\" }");
    }
    
   
    protected void doDeleteUser (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id)
    throws ServletException, IOException
    {
        User.delete(getContentResolver(), id);
        doBaseContent(writer, request, response);
    }
    
    
    protected void doSaveUserJSON (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id)
    throws ServletException, IOException
    {
        if (!true)
        {            
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        response.setStatus(HttpServletResponse.SC_OK);
        writer.println("{ \"status\": \"OK\" }");
    }
        
    
    /**
     * doSaveUser
     * 
     * Save edited or added User info.
     * 
     * @param writer
     * @param request
     * @param response
     * @param id
     * @throws ServletException
     * @throws IOException
     */
    protected void doSaveUser (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id)
    throws ServletException, IOException
    {
        ContentValues person = new ContentValues();
        person.put(Contacts.People.NAME, request.getParameter("name"));
        person.put(Contacts.People.NOTES, request.getParameter("notes"));
        person.put(Contacts.People.STARRED, request.getParameter("starred") != null ? 1 : 0);
        
        id = (id == null? id : id.trim());
        id = (id == null? id : ("".equals(id) ? null : id));

        if (id != null)
        {   
            User.save(getContentResolver(), person, id);
            Log.i("Jetty", "Updating user id "+id);
        }
        else
        {
            id = User.create(getContentResolver(), person);
            Log.i("Jetty", "Inserted new user id "+id);
        }
        
        //show the details for a particular user
        response.setStatus(HttpServletResponse.SC_OK);
        if (isMobileClient(request)) {
            // Go back to the user's page, since we don't like iframes on
            // tiny screens.
            doUserContent(writer, request, response, id);
        }
        else
        {
            // Just go back to the user selection page (the one with an iframe).
            doBaseContent(writer, request, response);
        }
    }
    
    

    protected void doContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException
    {
        doBaseContent(writer, request, response);
    }
    
    

    /**
     * doBaseContent
     * 
     * Output an overview page of all the Users.
     * 
     * @param writer
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doBaseContent(PrintWriter writer, HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException
    {
        writer.println("<h1>Contact List</h1><div id='content'>");
        
        User.UserCollection users =  User.getAll(getContentResolver());

        formatUserDetails (users, writer);
        users.close();

        writer.println("<br /><a href=\"/console/contacts?action="+__ACTION_ADD+"\"><button id='add'>Add</button></a>");        
        writer.println("</div>");
    }
    
    protected void doUserContentJSON (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {
        //query for the user's standard details
        writer.print("{ \"summary\" : ");
        ContentValues values = User.get(getContentResolver(), who);
        formatSummaryUserDetailsJSON (values, writer);
        
        writer.print(", \"phones\" : ");
        
        //query for all phone details
        User.PhoneCollection phones = User.getPhones(getContentResolver(), who);
        formatPhonesJSON (who, phones, writer);
        phones.close();
        
        writer.print(", \"addresses\" : ");
        
        //query for all contact details
        User.ContactMethodsCollection contactMethods = User.getContactMethods(getContentResolver(), who);
        formatContactMethodsJSON (who, contactMethods, writer);
        contactMethods.close(); 
        
        writer.print(" }");
    }
    
    
    /**
     * doUserContent
     * 
     * Output some information on a particular User, such as phones, email addresses etc.
     * @param writer
     * @param request
     * @param response
     * @param who
     * @throws ServletException
     * @throws IOException
     */
    protected void doUserContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {           
        //query for the user's standard details
        ContentValues values = User.get(getContentResolver(), who);
        formatSummaryUserDetails (values, writer);
       
        //query for all phone details
        User.PhoneCollection phones = User.getPhones(getContentResolver(), who);
        formatPhones (who, phones, writer);
        phones.close();
        
        //query for all contact details
        User.ContactMethodsCollection contactMethods = User.getContactMethods(getContentResolver(), who);
        formatContactMethods (who, contactMethods, writer);
        contactMethods.close(); 
        
        writer.println("<br /><a target='_top' href='/console/contacts/"+who+"?action="+__ACTION_EDIT+"'><button id='edit'>Edit</button></a>&nbsp;<a target='_top' href=\"/console/contacts/"+who+"?action="+__ACTION_DEL+"\"><button id='del'>Delete</button></a>");
    }
    
    /**
     * doEditUser
     * 
     * Output an edit form for a User.
     * 
     * @param writer
     * @param request
     * @param response
     * @param id
     * @throws ServletException
     * @throws IOException
     */
    private void doEditUser (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id) 
    throws ServletException, IOException
    {
        String name = "";
        String notes = "";
        boolean starred = false;
        
        boolean editing = !(id == null || id.trim().equals(""));
        
        if (editing)
        {
            writer.println("<h1>Editing contact</h1>");
        } 
        else
        {
            writer.println("<h1>Adding contact</h1>");
        }
            
        
        writer.println("<div id='content'>");
        
        writer.println("<form action=\"/console/contacts?action="+__ACTION_SAVE+"\" method='post'>");
        if (id != null)
            writer.println("<input type='hidden' name='id' value='" + id + "'>");
        writer.println("<table>");

        if (editing)
        {
            ContentValues user = User.get(getContentResolver(), id);
            if (user != null)
            {
                name = user.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
                notes = user.getAsString(Contacts.PeopleColumns.NOTES);
                Integer i = user.getAsInteger(Contacts.PeopleColumns.STARRED);
                Log.i("Jetty", "User starring = "+i);
                starred = (i == null ? false : i.intValue() > 0);
            }            
        }
        
        writer.println("<tr><td colspan='2'><h2>General</h2></td></tr>");
        writer.println("<tr><td>Name: </td><td><input name='name' type='text' value='" + name + "' /></td></tr>");
        writer.println("<tr><td>Starred: </td><td><input name='starred' type='checkbox' " + (starred ? "checked='checked'" : "") + " /></td></tr>");
        writer.println("<tr><td>Notes: </td><td><textarea name='notes'>" + (notes != null ? notes : "") + "</textarea></td></tr>");
           
        writer.println("</table>");
        
        writer.println("<br /><button id='save'>Save</button></form>");
        writer.println("</div>");
    }


    /**
     * formatUserDetails
     * 
     * For a set of users, print out a 1 line of data.
     * 
     * @param users
     * @param writer
     */
    private void formatUserDetails (User.UserCollection users, PrintWriter writer)
    {
        if (users!=null && writer!=null)
        {            
            writer.println("<table id='user'>");
            writer.println("<thead><tr>");
            writer.println("<th>Starred</th><th>Photo</th><th>Name</th>");
            writer.println("</tr></thead><tbody>");
            
            int row = 0;
            ContentValues user = null;
            while ((user = users.next()) != null)
            {  
                String style = getRowStyle(row);           
                writer.println("<tr"+style+">");

                String id = user.getAsString(android.provider.BaseColumns._ID);  
                String name =  user.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
                String title = null;
                String company = null;
                String notes = user.getAsString(Contacts.PeopleColumns.NOTES);
                Integer i = user.getAsInteger(Contacts.PeopleColumns.STARRED);
                Log.i("Jetty", "On read user starring = "+i);
                boolean starred = (i == null ? false : i.intValue() > 0);
                printCell (writer, (starred?"<span class='big'>*</span>":"&nbsp;"), style);

                printCell(writer, "<a class='userlink' href='/console/contacts/"+id+"/'><img src=\"/console/contacts/"+id+"/photo\""+" /></a>", style);
                printCell(writer, "<a class='userlink' href=\"/console/contacts/"+id+"\">"+name+"</a>", style);
                writer.println("</tr>");
                ++row;
            }
            writer.println("</tbody></table>");

            if (row==0)
            {
                writer.println("<h2 style='text-align: center;'>Sorry, you haven't added any contacts to your phone!</h2>");
            }
        }
    }
    
    /**
     * formtaSummaryUserDetails
     * 
     * For a given user, write out all the info we know about them.
     * 
     * @param values
     * @param writer
     */
    private void formatSummaryUserDetails (ContentValues values, PrintWriter writer)
    {
        if (values!=null && writer!=null)
        {  
            String id = values.getAsString(android.provider.BaseColumns._ID);  
            String name =  values.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
            String title = null;
            String company = null;
            String notes = values.getAsString(Contacts.PeopleColumns.NOTES);

            Integer i = values.getAsInteger(Contacts.PeopleColumns.STARRED);   
            Log.i("Jetty", "On summary starring = "+i);
            boolean starred = (i == null ? false : i.intValue() > 0);
            
            writer.println("<h1>"+(starred?"<span class='big'>*</span>&nbsp;":"")+(title==null?"":title+"&nbsp;")+(name==null?"Unknown":name)+"</h1>");
            
            writer.println("<div id='content'>");
            writer.println("<h2>Photo</h2><a href='/console/contacts/"+id+"/photo'><img src=\"/console/contacts/"+id+"/photo\""+"/></a>");
            if (company!=null)
                writer.println("<p>Company: "+company+"</h3></p>");
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
    
    private void formatSummaryUserDetailsJSON (ContentValues values, PrintWriter writer)
    {
        if (values!=null && writer!=null)
        {  
            String id = values.getAsString(android.provider.BaseColumns._ID);  
            String name =  values.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
            String title = null;
            String company = null;
            String notes = values.getAsString(Contacts.PeopleColumns.NOTES);

            Integer i = values.getAsInteger(Contacts.PeopleColumns.STARRED);   
            boolean starred = (i == null ? false : i.intValue() > 0);
            String starredStr = new Boolean(starred).toString();
            
            writer.print ("{ 'name': '" + name + "', ");
            writer.print ("'starred': " + starredStr);
            
            if (company != null)
                writer.print (", 'company': '" + company + "'");
                
            if (notes != null)
                writer.print (", 'notes': '" + notes + "'");
            
            writer.print (" }");
        }
    }
    

    private void formatPhones (String who, PhoneCollection phones, PrintWriter writer)
    {
        writer.println("<h2>Phone Numbers</h2>");
        writer.println("<table id='phones' style='border: 0px none;'>");
        int row = 0;
        ContentValues phone;
        while ((phone = phones.next() ) != null)
        {  
            String style = getRowStyle(row);
            writer.println("<tr class='"+style+"'>");
            String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
            String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
            int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
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
            printCell(writer, (number==null?"&nbsp;":"<a href=\"/console/contacts/"+who+"?action="+__ACTION_CALL+"&number="+encodedNumber+"\">"+number+"</a>&nbsp;<span class='qualifier'>["+phoneType+"]</span>"), style);
            writer.println("</tr>");
            row++;
        }
        writer.println("</table>");
    }
    
    private void formatPhonesJSON (String who, PhoneCollection phones, PrintWriter writer)
    {
        ContentValues phone;
        writer.print ("{ ");
        ArrayList<String> numbers = new ArrayList<String>();
        
        while ((phone = phones.next() ) != null)
        {  
            String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
            String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
            int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
            String phoneType=(String)_phoneTypes.get(Integer.valueOf(type));
            
            numbers.add ("'" + number + "' : { 'label' : '" + (label == null ? "" : label) + "', 'type' : '" + phoneType + "' }");
        }
        
        String lastNumber;
        
        if (numbers.size () > 1)
        {
            lastNumber = numbers.remove (numbers.size() - 1);
            
            for (String number : numbers)
            {
                writer.print (number + ", ");
            }
            
            // Last item shouldn't end with comma
            writer.print (lastNumber);
        }
        
        writer.print(" }");
    }
    

    private void formatContactMethods (String who, User.ContactMethodsCollection contactMethods, PrintWriter writer)
    {
        writer.println("<h2>Addresses</h2>");
        writer.println("<table id='addresses' style='border: 0px none;'>");
        int row = 0;
        ContentValues contactMethod;
        
        while ((contactMethod = contactMethods.next()) != null)
        { 
            String style = getRowStyle(row);
            writer.println("<tr class='"+style+"'>");
            String data = contactMethod.getAsString(Contacts.ContactMethodsColumns.DATA);
            String auxData = contactMethod.getAsString(Contacts.ContactMethodsColumns.AUX_DATA);
            String label = contactMethod.getAsString(Contacts.ContactMethodsColumns.LABEL);
            int isPrimary = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.ISPRIMARY).intValue();
            int kind = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.KIND).intValue();
            int type = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.TYPE).intValue();
            String typeStr;
            switch (type)
            {
                case Contacts.ContactMethodsColumns.TYPE_CUSTOM:
                {
                  typeStr = label;
                  break;
                }
                default:
                {
                  typeStr = (String)_contactEmailTypes.get(Integer.valueOf(type));
                }
            }

            switch (kind)
            {
                case Contacts.KIND_EMAIL:
                {
                        printCell(writer, "<span class='label'>"+__EMAIL+":</span>", style);
                        printCell(writer, "<a href=\"mailto:"+data+"\">"+data+" ["+typeStr+"]</a>", style);
                        printCell(writer, "", style);
                        break;
                }
                case Contacts.KIND_IM:
                {
                        printCell(writer, "<span class='label'>"+__IM+":</span>", style);
                        printCell(writer, data, style);
                        printCell(writer, "", style);
                        break;
                }
                case Contacts.KIND_POSTAL:
                {
                        printCell(writer, "<span class='label'>"+__POSTAL+":</span>", style);
                        printCell(writer, data+"&nbsp;<span class='qualifier'>["+typeStr+"]</span>", style);
                        printCell(writer, "", style);
                        break;
                }
/*
                case Contacts.ContactMethodsColumns.LOCATION_KIND:
                {
                        printCell(writer, "<span class='label'>"+__GEO_LOCATION+":</span>", style);
                        String typeStr = (String)_contactEmailTypes.get(Integer.valueOf(type));
                        printCell(writer, "<span class='qualifier'>["+typeStr+"]</span>&nbsp;"+data, style);
                        printCell(writer, auxData, style);
                        break;
                }
*/
                default:
                {
                        if (data!=null)
                                printCell(writer, data, style);
                        if (auxData!=null)
                                printCell(writer, data, style);
                        
                        printCell (writer, "Kind = " + kind + "; type = " + type, style);
                        break;
                }
            }
            writer.println("</tr>");
            row++;
        }
        
        writer.println("</table>");
    }  
    
    private void formatContactMethodsJSON (String who, User.ContactMethodsCollection contactMethods, PrintWriter writer)
    {
        writer.print ("[ ]");
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
