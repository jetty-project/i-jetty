//========================================================================
//$Id$
//Copyright 2009 Mort Bay Consulting Pty. Ltd.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.URIUtil;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.Contacts;
import android.util.Log;





/**
 * AbstractContactsServlet
 *
 * Restful request uri structure:
 * 
 *  /contacts/json/[who]/[what][?action=0|1|2|3|4][&id=x]
 *  
 *  what = "photo"|""
 *  who = contact db id
 *  
 *  eg request to retrieve Contact information about Contact 123:
 *  
 *   /contacts/json/123
 *   
 * eg request to delete Contact 123:
 * 
 *   /contacts/json/123?action=3
 *   
 * eg request to save a new Contact:
 *   /contacts/json?action=4
 *   
 *   
 * eg request to saved updated Contact:
 *   /contacts/json/123?action=1&id=123
 *   
 * eg request to retrieve 10 contacts starting at Contact 25:
 *   /contacts/json?start=25&pg=10
 *   
 * eg request to retrieve all contacts:
 *   /contacts/json
 *   
 */
public abstract class AbstractContactsServlet extends HttpServlet
{
    private static final String TAG = "ContactsServlet";
    private static final long serialVersionUID = 1L;
    public static int __VERSION = 0;

    /*
     * parameter "action" values
     */
    public static final int __ACTION_NONE = -1;
    public static final int __ACTION_CALL = 0;
    public static final int __ACTION_EDIT = 1;
    public static final int __ACTION_ADD = 2;
    public static final int __ACTION_DEL = 3;
    public static final int __ACTION_SAVE = 4;
    
    public static final int __DEFAULT_PG_START = 0;
    public static final int __DEFAULT_PG_SIZE = 10;
    
    
    public static final String __ACTION_PARAM = "action";
    public static final String __PG_START_PARAM = "pgStart";
    public static final String __PG_SIZE_PARAM = "pgSize";

    private ContentResolver resolver;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String pathInfo = request.getPathInfo();
        String servletPath = request.getServletPath();
        String pathInContext = URIUtil.addPaths(servletPath,pathInfo);

        if (pathInfo == null)
        {
            //redirect any requests for /console/contacts to be /console/contacts/
            RequestDispatcher dispatcher = request.getRequestDispatcher(pathInContext + "/");
            dispatcher.forward(request,response);
        }
        else
        {
            String who = null;
            String what = null;
            boolean json = false;

            StringTokenizer strtok = new StringTokenizer(pathInfo,"/");
            if (strtok.hasMoreElements())
            {
                who = strtok.nextToken();
            }

            if (strtok.hasMoreElements())
            {
                what = strtok.nextToken();
            }

            String str = request.getParameter(__ACTION_PARAM);
            int action = (str == null?__ACTION_NONE:Integer.parseInt(str.trim()));

            str = request.getParameter("json");
            json = (str == null?false:Integer.parseInt(str.trim()) == 1);

            Log.d(TAG,"who=" + who + " what=" + what + " action=" + action + " json=" + json);

            switch (action)
            {
                case __ACTION_NONE:
                {
                    if ((what == null) && (who != null))
                    {
                        //
                        // No specific action, but a Contact, so return their details
                        //
                        handleGetContact(request,response,who);
                    }
                    else if ((what == null) && (who == null))
                    {
                        //
                        // No action and no specific Contact, return a list of all Contacts
                        //
                        String tmp = request.getParameter(__PG_START_PARAM);
                        int pgStart = (tmp == null ? -1 : Integer.parseInt(tmp.trim()));
                        tmp = request.getParameter(__PG_SIZE_PARAM);
                        int pgSize = (tmp == null ? -1 : Integer.parseInt(tmp.trim()));
                        
                        handleGetContacts(request,response, pgStart, pgSize);
                    }
                    else if ("photo".equals(what.trim()))
                    {
                        //
                        // Handle responding with an image
                        //
                        handleGetImage(request,response,who);
                    }
                    break;
                }
                case __ACTION_CALL:
                {
                    //
                    // Call a Contact
                    // TODO not implemented
                    //

                    break;
                }
                case __ACTION_ADD:
                {
                    //
                    //Handle request to put up a form to add a new Contact
                    //
                    handleAddContact(request,response);

                    break;
                }
                case __ACTION_EDIT:
                {
                    //
                    // Handle request to edit a Contact
                    //
                    handleEditContact(request,response,who);

                    break;
                }
                case __ACTION_SAVE:
                {
                    //
                    // Save a Contact, either new or edited
                    //
                    handleSaveContact(request,response,request.getParameter("id"));

                    break;
                }
                case __ACTION_DEL:
                {
                    //
                    // Delete a contact
                    //
                    handleDeleteContact(request,response,who);

                    break;
                }
                default:
                {
                    handleDefault(request,response);
                }
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request,response);
    }

    public ContentResolver getContentResolver()
    {
        return resolver;
    }

    public abstract void handleAddContact(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

    public abstract void handleDefault(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

    public abstract void handleDeleteContact(HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException;

    public abstract void handleEditContact(HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException;

    public void handleGetImage(HttpServletRequest request, HttpServletResponse response, String who) throws IOException
    {
        Uri personUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI,Long.valueOf(who.trim()).longValue());
        InputStream is = Contacts.People.openContactPhotoInputStream(getContentResolver(),personUri);
        if (is == null)
        {
            response.setContentType("image/jpeg");
            OutputStream os = response.getOutputStream();
            is = getServletContext().getResourceAsStream("/android.jpg");
            IO.copy(is,os);
        }
        else
        {
            response.setContentType("image/png");
            OutputStream os = response.getOutputStream();
            IO.copy(is,os);
        }
    }

    public abstract void handleGetContact(HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException;

    public abstract void handleGetContacts(HttpServletRequest request, HttpServletResponse response, int pgStart, int pgSize) throws ServletException, IOException;

    public abstract void handleSaveContact(HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
    }

    public void saveContactFormData(HttpServletRequest request, HttpServletResponse response, String id) throws ServletException, IOException
    {
        ContentValues person = new ContentValues();
        person.put(Contacts.People.NAME,request.getParameter("name"));
        person.put(Contacts.People.NOTES,request.getParameter("notes"));
        person.put(Contacts.People.STARRED,request.getParameter("starred") != null?1:0);
        person.put(Contacts.People.SEND_TO_VOICEMAIL,request.getParameter("voicemail") != null?1:0);

        id = (id == null?id:id.trim());
        id = (id == null?id:("".equals(id)?null:id));

        Log.i(TAG,"Saving: name=" + request.getParameter("name") + " notes=" + request.getParameter("notes") + " id=" + id + " starred="
                + request.getParameter("starred"));

        if (id == null)
        {
            // Create it first if necessary (so we can save phone data)
            id = Contact.create(getContentResolver(),person);
            Log.d(TAG,"Inserted new Contact id " + id);
        }

        File photo = (File)request.getAttribute("new-pic");
        if (photo != null)
        {
            //a new picture for the Contact has been uploaded
            Contact.savePhoto(getContentResolver(),id,photo);
        }
        List<String> deletedPhones = new ArrayList<String>();
        Map<String, ContentValues> modifiedPhones = new HashMap<String, ContentValues>();
        List<String> deletedContacts = new ArrayList<String>();
        Map<String, ContentValues> modifiedContacts = new HashMap<String, ContentValues>();
        Enumeration<?> enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements())
        {
            String name = (String)enumeration.nextElement();
            if (name.startsWith("phone-del-"))
            {
                //a phone to delete
                String phId = name.substring(10);
                Log.d(TAG,"Phone to delete: " + phId + " from " + name);
                String val = request.getParameter(name);
                if ("del".equals(val))
                {
                    deletedPhones.add(phId);
                }
            }
            else if (name.startsWith("contact-del-"))
            {
                String methodId = name.substring(12);
                Log.d(TAG,"Contact method to delete: " + methodId + " from " + name);
                String val = request.getParameter(name);
                if ("del".equals(val))
                {
                    deletedContacts.add(methodId);
                }
            }
            else if (name.startsWith("phone-type-"))
            {
                String phId = name.substring(11);
                if (request.getParameter("phone-del-" + phId) == null)
                {
                    Log.d(TAG,"Modified phone " + phId + " from " + name);
                    String typeStr = request.getParameter(name);
                    String number = request.getParameter("phone-number-" + phId);
                    ContentValues phone = new ContentValues();
                    phone.put(Contacts.Phones.NUMBER,number);
                    phone.put(Contacts.Phones.TYPE,typeStr);
                    modifiedPhones.put(phId,phone);
                }
            }
            else if (name.startsWith("contact-kind-"))
            {
                String methodId = name.substring(13);
                Log.d(TAG,"Possible contact modification: " + methodId);
                if (request.getParameter("contact-del-" + methodId) == null)
                {
                    String kind = request.getParameter(name);
                    String type = request.getParameter("contact-type-" + methodId);
                    String val = request.getParameter("contact-val-" + methodId);
                    ContentValues contactMethod = new ContentValues();
                    contactMethod.put(Contacts.ContactMethodsColumns.KIND,kind);
                    contactMethod.put(Contacts.ContactMethodsColumns.TYPE,type);
                    contactMethod.put(Contacts.ContactMethodsColumns.DATA,val);
                    Log.d(TAG,"Modified contact " + methodId + " kind=" + kind + " type=" + type + " val=" + val);
                    modifiedContacts.put(methodId,contactMethod);
                }
            }
        }

        //Handle addition and modifications to phones
        for (String key : modifiedPhones.keySet())
        {
            ContentValues phone = modifiedPhones.get(key);
            if ("x".equals(key))
            {
                //new phone, check a number has been given
                String number = phone.getAsString(Contacts.Phones.NUMBER);
                if ((number != null) && !"".equals(number))
                {
                    Log.d(TAG,"Adding new phone with number " + number);
                    Phone.addPhone(getContentResolver(),phone,id);
                }
            }
            else
            {
                //possibly modified phone, save anyway
                Log.d(TAG,"Saving phone id=" + key);
                Phone.savePhone(getContentResolver(),phone,key,id);
            }
        }
        //Get rid of the deleted phones
        for (String phId : deletedPhones)
        {
            Phone.deletePhone(getContentResolver(),phId,id);
            Log.d(TAG,"Deleted phone " + phId);
        }

        //Handle addition and modifications to contacts
        for (String key : modifiedContacts.keySet())
        {
            ContentValues contactMethod = modifiedContacts.get(key);
            if ("x".equals(key))
            {
                //could be a new contact method, check if any data has been provided
                String data = contactMethod.getAsString(Contacts.ContactMethodsColumns.DATA);
                Log.d(TAG,"Data for new contact method : " + data);
                if ((data != null) && !"".equals(data))
                {
                    Log.d(TAG,"Adding new contact method with data " + data);
                    ContactMethod.addContactMethod(getContentResolver(),contactMethod,id);
                }
            }
            else
            {
                //modified contact method, save it
                Log.d(TAG,"Saving contact method " + key);
                ContactMethod.saveContactMethod(getContentResolver(),contactMethod,key,id);
            }
        }

        //Get rid of deleted contacts
        for (String methodId : deletedContacts)
        {
            ContactMethod.deleteContactMethod(getContentResolver(),methodId,id);
        }

        Contact.save(getContentResolver(),person,id);
        __VERSION++;
        Log.d(TAG,"Updating Contact id " + id);
    }
}
