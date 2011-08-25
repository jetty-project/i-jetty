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
import java.io.PrintWriter;
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
 * ContactsJSONServlet
 *
 * A servlet to support restful style requests for Android Contacts.
 * Restful request uri structure:
 * 
 *  /rest/contacts/photo/[who]
 *  A request to obtain the photo corresponding to contact [who]
 *  eg
 *  /rest/contacts/photo/123
 *
 *  /rest/contacts/[who]/[what][?action=0|1|2|3|4][&id=x]
 *  
 *  who = contact db id
 *  
 *  eg request to retrieve Contact information about Contact 123:
 *  
 *   /rest/contacts/123
 *   
 * eg request to delete Contact 123:
 * 
 *   /rest/contacts/123?action=3
 *   
 * eg request to save a new Contact:
 *   /rest/contacts/?action=4
 *   
 *   
 * eg request to saved updated Contact:
 *   /rest/contacts?action=1&id=123
 *   
 * eg request to retrieve 10 contacts starting at Contact 25:
 *   /rest/contacts?start=25&pg=10
 *   
 * eg request to retrieve all contacts:
 *   /rest/contacts/
 *   
 * 
 */
public class ContactsJSONServlet extends HttpServlet
{  
    private static final String TAG = "ContactsSrvlt";
    private static final long serialVersionUID = 1L;
    public static int __VERSION = 0;
    
    
    public static final int __ACTION_NONE = -1;
    public static final int __ACTION_CALL = 0;
    public static final int __ACTION_DEL = 3;
    public static final int __ACTION_SAVE = 4;
    
    public static final int __DEFAULT_PG_START = 0;
    public static final int __DEFAULT_PG_SIZE = 10;
    
    
    public static final String __ACTION_PARAM = "action";
    public static final String __PG_START_PARAM = "pgStart";
    public static final String __PG_SIZE_PARAM = "pgSize";
    
    private ContentResolver resolver;
    
    
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
    }

    
    public ContentResolver getContentResolver()
    {
        return resolver;
    }
    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request,response);
    }
    
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
            StringTokenizer strtok = new StringTokenizer(pathInfo,"/");
            
            String who = null;
            boolean isPhoto = false;
            int action = __ACTION_NONE;
            
            if (strtok.hasMoreElements())
            {
                String tmp = strtok.nextToken();
                
                if ("photo".equalsIgnoreCase(tmp))
                    isPhoto = true;
                else
                    who = tmp;
            }
            
            if (who == null && strtok.hasMoreElements())
            {
                who = strtok.nextToken();
            }

            String tmp = request.getParameter(__ACTION_PARAM);
            action = (tmp == null?__ACTION_NONE:Integer.parseInt(tmp.trim()));
          
            switch (action)
            {
                case __ACTION_NONE:
                {
                    if (who != null)
                    {
                        //a specific contact is being requested
                        if (isPhoto)
                            handleGetImage(request,response,who);
                        else
                            handleGetContact(request,response,who);
                    }
                    else
                    {
                        //Get all contacts
                        String str = request.getParameter(__PG_START_PARAM);
                        int pgStart = (str == null ? -1 : Integer.parseInt(str.trim()));
                        str = request.getParameter(__PG_SIZE_PARAM);
                        int pgSize = (str == null ? -1 : Integer.parseInt(str.trim()));
                        
                        handleGetContacts(request,response, pgStart, pgSize);
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
                case __ACTION_SAVE:
                {
                    //
                    // Save a Contact, either new or edited
                    //
                    who = request.getParameter("id");
                    handleSaveContact(request,response,who);

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
    
    
    protected void deleteUser(PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id) throws ServletException, IOException
    {
        try
        {
            Contact.delete(getContentResolver(),id);
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

    private void getContactMethods(String who, ContactMethod.ContactMethodsCollection contactMethods, PrintWriter writer)
    {
        writer.print("[");
        ContentValues contactMethod;
        StringBuffer buff = new StringBuffer();
        while (contactMethods != null && (contactMethod = contactMethods.next()) != null)
        {
            String id = contactMethod.getAsString(android.provider.BaseColumns._ID);
            String data = contactMethod.getAsString(Contacts.ContactMethodsColumns.DATA);
            String auxData = contactMethod.getAsString(Contacts.ContactMethodsColumns.AUX_DATA);
            String label = contactMethod.getAsString(Contacts.ContactMethodsColumns.LABEL);
            int isPrimary = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.ISPRIMARY).intValue();
            int kind = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.KIND).intValue();
            int type = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.TYPE).intValue();
            buff.append("{ \"data\": \"" + data + "\", \"aux\": \"" + auxData + "\",  \"label\": \"" + (label == null?"\"":label + "\"") + ", \"primary\": "
                    + isPrimary + ", \"kind\": " + kind + ", \"type\": " + type + ", \"id\": \"" + id + "\"}");
            if (contactMethods.hasNext())
            {
                buff.append(",");
            }
        }
        writer.print(buff.toString());
        writer.print("]");
    }

    private void getPhones(String who, Phone.PhoneCollection phones, PrintWriter writer)
    {
        ContentValues phone;
        writer.print("[ ");
        StringBuffer buff = new StringBuffer();
        while (phones != null && (phone = phones.next()) != null)
        {
            String id = phone.getAsString(android.provider.BaseColumns._ID);
            String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
            String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
            int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
            buff.append("{\"number\": \"" + number + "\", \"label\" : \"" + (label == null?"":label.replace("\'","\\'")) + "\", \"type\" : " + type
                    + ", \"id\": \"" + id + "\" }");
            if (phones.hasNext())
            {
                buff.append(", ");
            }
        }
        writer.print(buff.toString());
        writer.print(" ]");
    }

    private void getSummary(ContentValues values, PrintWriter writer)
    {
        if ((values != null) && (writer != null))
        {
            String id = values.getAsString(android.provider.BaseColumns._ID);
            String name = values.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
            String notes = values.getAsString(Contacts.PeopleColumns.NOTES);
            Integer i = values.getAsInteger(Contacts.PeopleColumns.STARRED);
            boolean starred = (i == null?false:i.intValue() > 0);
            i = values.getAsInteger(Contacts.PeopleColumns.SEND_TO_VOICEMAIL);
            boolean voicemail = (i == null?false:i.intValue() > 0);

            writer.print("{\"name\": \"" + name.replace("\'","\\'") + "\", ");
            writer.print("\"id\": \"" + id + "\", ");
            writer.print("\"starred\": " + Boolean.toString(starred) + ", ");
            writer.print("\"voicemail\": " + Boolean.toString(voicemail));
           

            if (notes != null)
            {
                writer.print(", \"notes\": \"" + notes.replace("'","\\'") + "\"");
            }

            writer.print(" }");
        }
    }

    /**
     * Generate a JSON representation of a Contact: eg.
     *
     * <pre>
     * {
     *  summary :    {
     *                name: "Fred Smith",
     *                id  : 123,
     *                starred: true/false,
     *                voicemail: true/false,
     *                notes: "Notes."
     *                },
     *  phones :      [
     *                  {
     *                    id: "98989",
     *                    number: "123456",
     *                    label: "Home",
     *                    type: "Home",
     *                    id:  900
     *                  },
     *                  {
     *                    id: "98989",
     *                    number: "989877",
     *                    label: "Work",
     *                    type: "Work",
     *                    id: 901
     *                  }
     *                ],
     *  contacts:     [
     *                  {
     *                    id: "99999",
     *                    data: "fred@smith.org",
     *                    aux: "",
     *                    label: "Home",
     *                    primary: true/false,
     *                    kind: "Email",
     *                    type:  "Home"
     *                  }
     *                ]
     * }
     *
     * </pre>
     *
     * @param writer
     * @param request
     * @param response
     * @param who
     * @throws ServletException
     * @throws IOException
     */
    protected void getContact (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {
        //query for the user's standard details
        ContentValues values = Contact.get(getContentResolver(),who);
        if (values == null)
            writer.println("{\"error\": \"No such user.\"}");
        else
        {
            writer.print("{ \"summary\" : ");
            getSummary(values,writer);

            writer.print(", \"phones\" : ");

            //query for all phone details
            Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(),who);
            getPhones(who,phones,writer);
            phones.close();

            writer.print(", \"contacts\" : ");

            //query for all contact details
            ContactMethod.ContactMethodsCollection contactMethods = ContactMethod.getContactMethods(getContentResolver(),who);
            getContactMethods(who,contactMethods,writer);
            contactMethods.close();

            writer.print(", \"version\": " + __VERSION);

            writer.print(" }");
        }
    }

    private void getContacts (Contact.ContactCollection users, PrintWriter writer, int pgSize)
    {        
        writer.println("{\"version\": " + __VERSION+", ");
        writer.println("\"total\": "+users.getTotal()+", ");
        writer.println("\"contacts\": ");
        writer.println("[");
        if (users != null)
        {
            ContentValues user = null;
            int count = pgSize;
           
            
            while ((pgSize <= 0 || count-- > 0) && (user = users.next()) != null)
            {
                writer.println("{");
                String id = user.getAsString(android.provider.BaseColumns._ID);
                String name = user.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
                Integer i = user.getAsInteger(Contacts.PeopleColumns.STARRED);
                boolean starred = (i == null?false:i.intValue() > 0);
                writer.println("\"id\" : \"" + id + "\", ");
                writer.println("\"name\" : \"" + name + "\", ");
                writer.println("\"starred\": " + starred);
                writer.println("}");
                if (users.hasNext())
                {
                    writer.println(",");
                }
            }
        }
        writer.println("]");
        writer.println("}");
    }

  


    public void handleDefault(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        handleGetContacts(request,response, __DEFAULT_PG_START, __DEFAULT_PG_SIZE);
    }


    public void handleDeleteContact(HttpServletRequest request, HttpServletResponse response, String who)
    throws ServletException, IOException
    {
        response.setContentType("text/json; charset=utf-8");
        PrintWriter writer = response.getWriter();
        deleteUser(writer,request,response,who);
    }


  
    public void handleGetContact(HttpServletRequest request, HttpServletResponse response, String who)
    throws ServletException, IOException
    {
        response.setContentType("text/json; charset=utf-8");
        PrintWriter writer = response.getWriter();
        getContact (writer,request,response,who);
        response.setStatus(HttpServletResponse.SC_OK);
        writer.write("\r\n");
        writer.close();
    }

 
    public void handleGetContacts(HttpServletRequest request, HttpServletResponse response,
                                  int pgStart, int pgSize)
    throws ServletException, IOException
    {
        PrintWriter writer = response.getWriter();
        response.setContentType("text/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        Contact.ContactCollection users = Contact.getContacts(getContentResolver(), pgStart, pgSize);

        getContacts(users,writer, pgSize);
        users.close();
    }

 
    public void handleSaveContact(HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {
        //Do NOT return any data. This is because the json form submission is
        //being sent to a hidden iframe otherwise the multipart mime enctype of
        //form would cause the browser to replace the whole content of the page
        //with the response.
        saveContactFormData(request,response,request.getParameter("id"));
    }
    
    public void handleGetImage(HttpServletRequest request, HttpServletResponse response, String who) throws IOException
    {
        InputStream is = null;

        try
        {
            Uri personUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI,Long.valueOf(who.trim()).longValue());
            is = Contacts.People.openContactPhotoInputStream(getContentResolver(),personUri);
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
        finally
        {
            if (is != null)
                is.close();
        }
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
            else if (name.startsWith("phone-number-"))
            {
                String phId = name.substring("phone-number-".length());
                if (request.getParameter("phone-del-" + phId) == null)
                {
                    String typeStr = request.getParameter("phone-type-"+phId);
                    String number = request.getParameter("phone-number-" + phId);
                    String label = request.getParameter("phone-type-label-"+phId);
                    Log.d(TAG, "Modified phone type="+typeStr+" number="+number+" label="+label);
                    ContentValues phone = new ContentValues();
                    phone.put(Contacts.Phones.NUMBER,number);
                    phone.put(Contacts.Phones.TYPE,typeStr);
                    if (label != null && !"".equals(label))
                        phone.put(Contacts.Phones.LABEL, label);
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
                    String label = request.getParameter("contact-type-label-"+methodId);
                    ContentValues contactMethod = new ContentValues();
                    contactMethod.put(Contacts.ContactMethodsColumns.KIND,kind);
                    contactMethod.put(Contacts.ContactMethodsColumns.TYPE,type);
                    contactMethod.put(Contacts.ContactMethodsColumns.DATA,val);
                    if (label != null && !"".equals(label))
                        contactMethod.put(Contacts.ContactMethodsColumns.LABEL, label);
                    Log.d(TAG,"Modified contact " + methodId + " kind=" + kind + " type=" + type + " val=" + val+" label="+label);
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
                    Log.d(TAG,"Adding new phone with number=" + number);
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
