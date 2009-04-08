package org.mortbay.ijetty.console;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentValues;
import android.provider.Contacts;

public class ContactsJSONServlet extends AbstractContactsServlet
{
    
    
    /**
     * Generate a JSON representation of a user:
     * eg.
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
     * @param writer
     * @param request
     * @param response
     * @param who
     * @throws ServletException
     * @throws IOException
     */
    protected void getUser(PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {
        //query for the user's standard details
        writer.print("{ \"summary\" : ");
        ContentValues values = User.get(getContentResolver(), who);
        getSummary (values, writer);
        
        writer.print(", \"phones\" : ");
        
        //query for all phone details
        Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(), who);
        getPhones (who, phones, writer);
        phones.close();
        
        writer.print(", \"contacts\" : ");
        
        //query for all contact details
        ContactMethod.ContactMethodsCollection contactMethods = ContactMethod.getContactMethods(getContentResolver(), who);
        getContactMethods (who, contactMethods, writer);
        contactMethods.close(); 
        
        writer.print(", \"version\": "+__VERSION);
        
        writer.print(" }");
    }
    
    private void getSummary (ContentValues values, PrintWriter writer)
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
            i = values.getAsInteger(Contacts.PeopleColumns.SEND_TO_VOICEMAIL);
            boolean voicemail = (i==null? false : i.intValue() > 0);
            
            writer.print ("{\"name\": \"" + name.replace("\'", "\\'") + "\", ");
            writer.print ("\"id\": \"" + id + "\", ");
            writer.print ("\"starred\": " + Boolean.toString(starred)+", ");
            writer.print ("\"voicemail\": "+Boolean.toString(voicemail));
            if (company != null)
                writer.print (", \"company\": \"" + company.replace("'", "\\'") + "\"");
                
            if (notes != null)
                writer.print (", \"notes\": \"" + notes.replace("'", "\\'") + "\"");
            
            writer.print (" }");
        }
    }
    
    private void getPhones (String who, Phone.PhoneCollection phones, PrintWriter writer)
    {
        ContentValues phone;
        writer.print ("[ ");
        StringBuffer buff = new StringBuffer();
        while ((phone = phones.next() ) != null)
        {  
            String id = phone.getAsString(android.provider.BaseColumns._ID);
            String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
            String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
            int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
            buff.append ("{\"number\": \""+number + "\", \"label\" : \"" + (label == null ? "" : label.replace("\'", "\\'")) + "\", \"type\" : " + type + ", \"id\": \""+id+"\" }");
            if (phones.hasNext())
                buff.append(", ");
        }
        writer.print(buff.toString());
        writer.print(" ]");
    }
    
    private void getContactMethods (String who, ContactMethod.ContactMethodsCollection contactMethods, PrintWriter writer)
    {
        writer.print ("[");
        ContentValues contactMethod;
        StringBuffer buff = new StringBuffer();
        while ((contactMethod = contactMethods.next()) != null)
        { 
            String id = contactMethod.getAsString(android.provider.BaseColumns._ID);
            String data = contactMethod.getAsString(Contacts.ContactMethodsColumns.DATA);
            String auxData = contactMethod.getAsString(Contacts.ContactMethodsColumns.AUX_DATA);
            String label = contactMethod.getAsString(Contacts.ContactMethodsColumns.LABEL);
            int isPrimary = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.ISPRIMARY).intValue();
            int kind = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.KIND).intValue();
            int type = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.TYPE).intValue();
            buff.append("{ \"data\": \""+data+"\", \"aux\": \""+auxData+"\",  \"label\": \""+(label==null?"\"":label+"\"") + ", \"primary\": "+isPrimary+", \"kind\": "+kind+", \"type\": "+type+", \"id\": \""+id+"\"}");
            if (contactMethods.hasNext())
                buff.append(",");
        }
        writer.print(buff.toString());
        writer.print ("]");
    }
    
    private void getUsers (User.UserCollection users, PrintWriter writer)
    {
        writer.println("{\"version\": "+__VERSION);
        writer.println(", \"users\": ");
        writer.println("[");
        if (users!=null)
        {            
            int row = 0;
            ContentValues user = null;
            while ((user = users.next()) != null)
            {  
                writer.println("{");
                String id = user.getAsString(android.provider.BaseColumns._ID);  
                String name =  user.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
                Integer i = user.getAsInteger(Contacts.PeopleColumns.STARRED);   
                boolean starred = (i == null ? false : i.intValue() > 0);
                writer.println("\"id\" : \""+id+"\", ");
                writer.println("\"name\" : \""+name+"\", ");
                writer.println("\"starred\": "+starred);
                writer.println("}"); 
                if (users.hasNext())
                    writer.println(",");
            }
        }
        writer.println("]");
        writer.println("}");
    }
    
    
    protected void deleteUser (PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id)
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


    public void handleAddUser(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        //No-op for JSON as putting up a form to add a user is handled by the front end
    }


    public void handleEditUser(HttpServletRequest request,
            HttpServletResponse response, String who) throws ServletException,
            IOException
    {
      //No-op for JSON as putting up a form to add a user is handled by the front end
    }


    public void handleDefault(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        handleGetUsers(request,response);
    }


    public void handleDeleteUser(HttpServletRequest request,
            HttpServletResponse response, String who) throws ServletException,
            IOException
    {
        response.setContentType("text/json; charset=utf-8");
        PrintWriter writer = response.getWriter(); 
        deleteUser(writer,request, response,who);
    }



    public void handleGetUser(HttpServletRequest request,
            HttpServletResponse response, String who) throws ServletException,
            IOException
    {
        response.setContentType("text/json; charset=utf-8");
        PrintWriter writer = response.getWriter(); 
        getUser(writer, request, response, who);
        response.setStatus(HttpServletResponse.SC_OK);
        writer.write("\r\n");
        writer.close();
    }


    public void handleGetUsers(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        PrintWriter writer = response.getWriter();
        response.setContentType("text/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        User.UserCollection users =  User.getAll(getContentResolver());

        getUsers(users, writer);
        users.close();
    }


    public void handleSaveUser(HttpServletRequest request,
            HttpServletResponse response, String who) throws ServletException,
            IOException
    {        
        //Do NOT return any data. This is because the json form submission is
        //being sent to a hidden iframe otherwise the multipart mime enctype of
        //form would cause the browser to replace the whole content of the page
        //with the response.
        saveUserFormData(request, response, request.getParameter("id"));
    }
}
