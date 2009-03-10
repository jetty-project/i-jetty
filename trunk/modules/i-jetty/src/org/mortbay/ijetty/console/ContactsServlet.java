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
import java.util.EnumMap;
import java.util.Enumeration;
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

import android.content.ContentUris;
import android.content.ContentValues;
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



    private static final String __POSTAL = "postal";
 
    private static final String __PRIMARY = "primary";
    private static final String __EMAIL = "email";

    private static final String __IM = "IM";
    private static final String __GEO_LOCATION = "geo location";
    
    private static enum __INFO_TYPE {Mobile, Home, Work, WorkFax, HomeFax, Pager, Other, Custom, Email, IM};
    private static EnumMap<__INFO_TYPE, String> __TYPE_LABELS = new EnumMap<__INFO_TYPE, String>(__INFO_TYPE.class);
    
    
    // FIXME: Use a local copy when finished testing. :)
    private static final String[] __JAVASCRIPT = new String[] { "http://jqueryjs.googlecode.com/files/jquery-1.3.min.js", "http://tablesorter.com/jquery.tablesorter.min.js", "http://www.appelsiini.net/download/jquery.jeditable.mini.js", "/console/contacts.js" };


    private Map<Integer, __INFO_TYPE> _phoneTypes = new HashMap<Integer, __INFO_TYPE>();
    private Map<Integer, __INFO_TYPE> _contactEmailTypes = new HashMap<Integer, __INFO_TYPE>();
    private Map _postalTypes = new HashMap();
    

    
    public ContactsServlet ()
    {
        __TYPE_LABELS.put(__INFO_TYPE.Mobile, "Mobile");
        __TYPE_LABELS.put(__INFO_TYPE.Home, "Home");
        __TYPE_LABELS.put(__INFO_TYPE.Work, "Work");
        __TYPE_LABELS.put(__INFO_TYPE.HomeFax, "Home Fax");
        __TYPE_LABELS.put(__INFO_TYPE.WorkFax, "Work Fax");
        __TYPE_LABELS.put(__INFO_TYPE.Pager, "Pager");
        __TYPE_LABELS.put(__INFO_TYPE.Other, "Other");
        __TYPE_LABELS.put(__INFO_TYPE.Custom, "Custom");
        
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_MOBILE), __INFO_TYPE.Mobile);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_HOME), __INFO_TYPE.Home);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_WORK), __INFO_TYPE.Work);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_FAX_WORK), __INFO_TYPE.WorkFax);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_FAX_HOME), __INFO_TYPE.HomeFax);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_PAGER), __INFO_TYPE.Pager);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_OTHER), __INFO_TYPE.Other);
        
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_HOME), __INFO_TYPE.Home);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_WORK), __INFO_TYPE.Work);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_OTHER), __INFO_TYPE.Other);
        _contactEmailTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_CUSTOM), __INFO_TYPE.Custom);
  
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
                        if (isMobileClient(request))
                            doHeader(writer, request, response);
                        else
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
                        //getContext()
                        
                        if (is == null)
                        {
                            //response.setContentType("application/octet-stream");
                            response.setContentType("image/jpeg");  
                            OutputStream os = response.getOutputStream();
                            is = getServletContext().getResourceAsStream("/android.jpg");
                            IO.copy(is,os);
                            //response.sendError(HttpStatus.SC_NOT_FOUND);
                        }
                        else
                        {
                            response.setContentType("image/png");
                            //response.setContentType("application/octet-stream");
                            //response.setContentType(getServletContext().getMimeType());
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
                        str = request.getParameter("another");
                        
                        response.setContentType("text/html");
                        response.setStatus(HttpServletResponse.SC_OK);
                        doHeader(writer, request, response);
                        doMenuBar(writer, request, response);
                        doSaveUser(writer, request, response, request.getParameter("id"), (str != null && str != ""));
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
                        if (isMobileClient(request))
                            doHeader(writer, request, response);
                        else
                            doHeader(writer, request, response, __JAVASCRIPT);
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
                    if (isMobileClient(request))
                        doHeader(writer, request, response);
                    else
                        doHeader(writer, request, response, __JAVASCRIPT);
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
    protected void doSaveUser (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id, boolean editing)
    throws ServletException, IOException
    {
        ContentValues person = new ContentValues();
        person.put(Contacts.People.NAME, request.getParameter("name"));
        person.put(Contacts.People.NOTES, request.getParameter("notes"));
        person.put(Contacts.People.STARRED, request.getParameter("starred") != null ? 1 : 0);
        
        id = (id == null? id : id.trim());
        id = (id == null? id : ("".equals(id) ? null : id));
        
        boolean created = false;
        
        if (id == null) {
            // Create it first if necessary (so we can save phone data)
            id = User.create(getContentResolver(), person);
            Log.i("Jetty", "Inserted new user id "+id);
            created = true;
        }
                
        // Loop through all our phone numbers that were POSTed     
        List<String> deletedPhones = new ArrayList<String>();
        
        Map<String, String[]> modifiedPhones = new HashMap<String, String[]>();
        Enumeration enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements())
        {
            String name = (String)enumeration.nextElement();
            if (name.startsWith("del-"))
            {
                //a phone to delete
                String phId = name.substring(4);
                Log.d("Jetty", "Phone to delete: "+phId+" from "+name);
                String val = request.getParameter(name);
                if ("del".equals(val))
                {
                    deletedPhones.add(phId);
                }
            }
            else if (name.startsWith("phone-type-"))
            { 
                String phId = name.substring(11);
                if (!deletedPhones.contains(phId))
                {
                    Log.d("Jetty", "Modified phone "+phId+" from "+name);
                    String typeStr = request.getParameter(name);
                    String number = request.getParameter("phone-number-"+phId);
                    String[] phInfo = new String[2];
                    phInfo[0] = typeStr;
                    phInfo[1] = number;
                    modifiedPhones.put(phId, phInfo);
                }
            }
        }
      
        //Handle addition and modifications
        for (String key: modifiedPhones.keySet())
        {
            String[] phInfo = modifiedPhones.get(key);
            if ("x".equals(key))
            {
                //new phone
                if (phInfo[1] != null && !"".equals(phInfo[1]))
                {
                    Log.d("Jetty", "Adding new phone with number "+phInfo[1]+" and type "+phInfo[1]);
                    ContentValues numberValues = new ContentValues();
                    numberValues.put(Contacts.Phones.NUMBER, phInfo[1]);
                    numberValues.put(Contacts.Phones.TYPE, phInfo[0]);
                    Phone.addPhone(getContentResolver(), numberValues, id);
                }
            }
            else
            {
                //possibly modified phone, save anyway
                Log.d("Jetty", "Saving phone id="+key+" with number "+phInfo[1]+" and type "+phInfo[0]);
                ContentValues numberValues = new ContentValues();
                numberValues.put(Contacts.Phones.NUMBER, phInfo[1]);
                numberValues.put(Contacts.Phones.TYPE, phInfo[0]);
                Phone.savePhone(getContentResolver(), numberValues, key, id);
            }
        }
    
        
        //Get rid of the deleted phones
        for (String phId : deletedPhones)
        {
            Phone.deletePhone(getContentResolver(), phId, id);
            Log.d("Jetty", "Deleted phone "+phId);
        }
        User.save(getContentResolver(), person, id);
        Log.i("Jetty", "Updating user id "+id);
        
        //show the details for a particular user
        response.setStatus(HttpServletResponse.SC_OK);
        if (!editing)
        {
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
        else
        {
            doEditUser(writer, request, response, id);
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
        // This HTML is used for the side pane for desktop browsers that use Javascript.
        writer.println("<h1 class='pageheader'>Contact List</h1><div id='content'>");
        writer.println("<div class='hidden' id='userinfo'><form onsubmit='return false;'>");
        writer.println("<input type='hidden' id='user-id' />");
        writer.println("<small id='user-edit-hint'>Click on some text to edit it.</small>");
        writer.println("<h1 style='margin-top: 2px; margin-bottom: 2px;' id='user-name'></h1>");
        writer.println("<small><div class='star' id='user-star'>*</div> | <a href='#' id='user-delete'>Delete</a> | <a href='#' id='user-call'>Call</a></small>");
        
        writer.println("<a href='#' id='user-photo-link'><img id='user-photo' alt='User photo' /></a>");
        
        writer.println("<h2 id='user-notes-label'>Notes</h2>");
        writer.println("<p id='user-notes'>Click to add a note.</p>");
        
        writer.println("<h2 id='user-numbers-label'><a href='#' id='user-numbers-add'><img src='/console/list-add.png' /></a>Phone Numbers</h2>");
        writer.println("<table id='user-numbers' class='hidden'></table>");
        
        writer.println("</form></div>");
        
        // Print out the table that everyone can read.
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
        Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(), who);
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
        Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(), who);
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
            writer.println("<h1 class='pageheader'>Editing contact</h1>");
        } 
        else
        {
            writer.println("<h1 class='pageheader'>Adding contact</h1>");
        }
            
        
        writer.println("<div id='content'>");
        
        writer.println("<form action=\"/console/contacts?action="+__ACTION_SAVE + "\" method='post'>");
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
        
        writer.println("<tr><td colspan='3'><h2>General</h2></td></tr>");
        writer.println("<tr><td>Name: </td><td colspan='2' ><input name='name' type='text' value='" + name + "' /></td></tr>");
        writer.println("<tr><td>Starred: </td><td colspan='2' ><input name='starred' type='checkbox' " + (starred ? "checked='checked'" : "") + " /></td></tr>");
        writer.println("<tr><td>Notes: </td><td colspan='2'  ><textarea name='notes'>" + (notes != null ? notes : "") + "</textarea></td></tr>");
        
        writer.println("<tr><td colspan='3'><h2>Phone numbers</h2></td></tr>");
        
        int phone_editor_id = 0;
        
        Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(), id);

        if (phones != null)
        {
            ContentValues phone;
            while ((phone = phones.next()) != null)
            {
                String phoneId = phone.getAsString(android.provider.BaseColumns._ID);
                String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
                String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
                int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
                writer.println(createPhoneEditor(phoneId, label,type,number));
                phone_editor_id++;
            }
        }


        //Put on a spare one so they can add another number
        writer.println(createPhoneEditor("x", "", -1, ""));
       // writer.println("<tr><td colspan='2'><input type='submit' name='another' id='another' value='Add another' /></form></td></tr>");
        writer.println("</table>");
        writer.println("<br /><input type='submit' name='save' id='save' value='Save' /> <a href='/console/contacts/" + (id != null ? id.toString() : "") + "'><button id='cancel'>Cancel</button></a></form>");
        writer.println("</div>");
    }
    
 

    
    private String createPhoneEditor (String id, String label, int type, String number)
    {        
        String select = "<select name='phone-type-" + id + "'>";
        String selected = " selected='selected'";

        for (Integer i : _phoneTypes.keySet())
        {
            __INFO_TYPE t = _phoneTypes.get(i);
            select += "<option value='" + i + "'" + (type == i.intValue() ? selected : "") + ">" + __TYPE_LABELS.get(t) + "</option>";
        }   
        select += "</select>";
        
        String row = "<tr>";
        if (!"x".equals(id))
            row += "<td><input type='checkbox' name='del-"+id+"' value='del'>Delete</input></td>";
        else
            row+= "<td>&nbsp;</td>";
        row += "<td>" + select + "</td><td><input type='text' name='phone-number-" + id + "' id='phone-number-" + id + "' style='width: 120px;' length='12' value='" + number + "' /></td></tr>";
        return row;
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

                String id = user.getAsString(android.provider.BaseColumns._ID);  
                writer.println("<tr"+style+" id='contact-" + id + "'>");
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
            
            writer.println("<h1 class='pageheader'>"+(starred?"<span class='big'>*</span>&nbsp;":"")+(title==null?"":title+"&nbsp;")+(name==null?"Unknown":name)+"</h1>");
            
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
            
            writer.print ("{ 'name': '" + name.replace("'", "\\'") + "', ");
            writer.print ("'id': " + id + ", ");
            writer.print ("'starred': " + starredStr);
            
            if (company != null)
                writer.print (", 'company': '" + company.replace("'", "\\'") + "'");
                
            if (notes != null)
                writer.print (", 'notes': '" + notes.replace("'", "\\'") + "'");
            
            writer.print (" }");
        }
    }
    

    private void formatPhones (String who, Phone.PhoneCollection phones, PrintWriter writer)
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
            String phoneType= __TYPE_LABELS.get(_phoneTypes.get(Integer.valueOf(type)));
            printCell(writer, phoneType, style);
            
            String encodedNumber = number;
            
            try
            {
                encodedNumber = URLEncoder.encode(number, "UTF-8");             
            }
            catch (Exception e)
            {
                Log.w("Jetty", "Encoding telephone number failed");
            }
            
            printCell(writer, (number==null?"Unknown":"<a href=\"/console/contacts/"+who+"?action="+__ACTION_CALL+"&number="+encodedNumber+"\">"+number+"</a>" + (label == null ? "" : "&nbsp;<span class='phone-label'>("+label+")</span>")), style);
            writer.println("</tr>");
            
            row++;
        }
        writer.println("</table>");
    }
    
    private void formatPhonesJSON (String who, Phone.PhoneCollection phones, PrintWriter writer)
    {
        ContentValues phone;
        writer.print ("{ ");
        ArrayList<String> numbers = new ArrayList<String>();
        
        while ((phone = phones.next() ) != null)
        {  
            String id = phone.getAsString(android.provider.BaseColumns._ID);
            String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
            String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
            int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
            numbers.add ("'" + number + "' : { 'label' : '" + (label == null ? "" : label.replace("'", "\\'")) + "', 'type' : " + type + ", 'id': '"+id+"' }");
        }
        
        String lastNumber;
        
        if (numbers.size () >= 1)
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
                  typeStr = __TYPE_LABELS.get(_contactEmailTypes.get(Integer.valueOf(type)));
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
