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
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentValues;
import android.provider.Contacts;
import android.util.Log;

public class ContactsHTMLServlet extends AbstractContactsServlet
{
    private static enum __INFO_TYPE
    {
        Mobile, Home, Work, WorkFax, HomeFax, Pager, Other, Custom, Email, IM, Postal, Phone, Organization
    }

    private static final String TAG = "ContactsHTMLServlet";

    private static final long serialVersionUID = 1L;
    private static EnumMap<__INFO_TYPE, String> __LABELS = new EnumMap<__INFO_TYPE, String>(__INFO_TYPE.class);

    private Map<Integer, __INFO_TYPE> _phoneTypes = new HashMap<Integer, __INFO_TYPE>();
    private Map<Integer, __INFO_TYPE> _contactTypes = new HashMap<Integer, __INFO_TYPE>();
    private Map<Integer, __INFO_TYPE> _contactKinds = new HashMap<Integer, __INFO_TYPE>();

    public ContactsHTMLServlet()
    {
        __LABELS.put(__INFO_TYPE.Mobile,"Mobile");
        __LABELS.put(__INFO_TYPE.Home,"Home");
        __LABELS.put(__INFO_TYPE.Work,"Work");
        __LABELS.put(__INFO_TYPE.HomeFax,"Home Fax");
        __LABELS.put(__INFO_TYPE.WorkFax,"Work Fax");
        __LABELS.put(__INFO_TYPE.Pager,"Pager");
        __LABELS.put(__INFO_TYPE.Other,"Other");
        __LABELS.put(__INFO_TYPE.Custom,"Custom");
        __LABELS.put(__INFO_TYPE.Phone,"Phone");
        __LABELS.put(__INFO_TYPE.Postal,"Postal");
        __LABELS.put(__INFO_TYPE.IM,"IM");
        __LABELS.put(__INFO_TYPE.Email,"Email");

        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_MOBILE),__INFO_TYPE.Mobile);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_HOME),__INFO_TYPE.Home);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_WORK),__INFO_TYPE.Work);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_FAX_WORK),__INFO_TYPE.WorkFax);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_FAX_HOME),__INFO_TYPE.HomeFax);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_PAGER),__INFO_TYPE.Pager);
        _phoneTypes.put(Integer.valueOf(Contacts.PhonesColumns.TYPE_OTHER),__INFO_TYPE.Other);

        _contactTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_HOME),__INFO_TYPE.Home);
        _contactTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_WORK),__INFO_TYPE.Work);
        _contactTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_OTHER),__INFO_TYPE.Other);
        _contactTypes.put(Integer.valueOf(Contacts.ContactMethodsColumns.TYPE_CUSTOM),__INFO_TYPE.Custom);

        _contactKinds.put(Integer.valueOf(Contacts.KIND_EMAIL),__INFO_TYPE.Email);
        _contactKinds.put(Integer.valueOf(Contacts.KIND_IM),__INFO_TYPE.IM);
        _contactKinds.put(Integer.valueOf(Contacts.KIND_PHONE),__INFO_TYPE.Phone);
        _contactKinds.put(Integer.valueOf(Contacts.KIND_POSTAL),__INFO_TYPE.Postal);
    }

    private String createContactMethodEditor(String id, int type, int kind, String label, String value)
    {
        String row = "<tr>";
        if (!"x".equals(id))
        {
            row += "<td><input type='checkbox' name='contact-del-" + id + "' value='del'>Delete</input></td>";
        }
        else
        {
            row += "<td>&nbsp;</td>";
        }

        String kindSelect = "<select name='contact-kind-" + id + "'>";
        for (Integer i : _contactKinds.keySet())
        {
            __INFO_TYPE t = _contactKinds.get(i);
            kindSelect += "<option value='" + i + "'" + (kind == i.intValue()?" selected='selected'":"") + ">" + __LABELS.get(t) + "</option>";
        }
        kindSelect += "</select>";

        String typeSelect = "<select name='contact-type-" + id + "'>";
        for (Integer i : _contactTypes.keySet())
        {
            __INFO_TYPE t = _contactTypes.get(i);
            typeSelect += "<option value='" + i + "'" + (type == i.intValue()?" selected='selected'":"") + ">" + __LABELS.get(t) + "</option>";
        }
        typeSelect += "</select>";

        row += "<td>" + kindSelect + "</td><td>" + typeSelect + "</td><td><input type='text' name='contact-val-" + id
                + "' style='width: 120px;' length='12' value='" + value + "'/></td>";
        row += "</tr>";
        return row;
    }

    private String createPhoneEditor(String id, String label, int type, String number)
    {
        String select = "<select name='phone-type-" + id + "'>";
        String selected = " selected='selected'";

        for (Integer i : _phoneTypes.keySet())
        {
            __INFO_TYPE t = _phoneTypes.get(i);
            select += "<option value='" + i + "'" + (type == i.intValue()?selected:"") + ">" + __LABELS.get(t) + "</option>";
        }
        select += "</select>";

        String row = "<tr>";
        if (!"x".equals(id))
        {
            row += "<td><input type='checkbox' name='phone-del-" + id + "' value='del'>Delete</input></td>";
        }
        else
        {
            row += "<td>&nbsp;</td>";
        }
        row += "<td>" + select + "</td><td><input type='text' name='phone-number-" + id + "' id='phone-number-" + id
                + "' style='width: 120px;' length='12' value='" + number + "' /></td></tr>";
        return row;
    }

    protected void doDeleteUser(PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id) throws ServletException, IOException
    {
        User.delete(getContentResolver(),id);
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
    private void doEditUser(PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String id) throws ServletException, IOException
    {
        String name = "";
        String notes = "";
        boolean starred = false;
        boolean voicemail = false;

        boolean editing = !((id == null) || id.trim().equals(""));

        if (editing)
        {
            writer.println("<h1 class='pageheader'>Editing contact</h1>");
        }
        else
        {
            writer.println("<h1 class='pageheader'>Adding contact</h1>");
        }

        writer.println("<div id='content'>");

        writer.println("<form action=\"/console/contacts/html?action=" + __ACTION_SAVE + "\" method='post' enctype='multipart/form-data'>");
        if (id != null)
        {
            writer.println("<input type='hidden' name='id' value='" + id + "'>");
        }
        writer.println("<table>");

        if (editing)
        {
            ContentValues user = User.get(getContentResolver(),id);
            if (user != null)
            {
                name = user.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
                notes = user.getAsString(Contacts.PeopleColumns.NOTES);
                Integer i = user.getAsInteger(Contacts.PeopleColumns.STARRED);
                starred = (i == null?false:i.intValue() > 0);
                i = user.getAsInteger(Contacts.PeopleColumns.SEND_TO_VOICEMAIL);
                voicemail = (i == null?false:i.intValue() > 0);
            }
        }

        writer.println("<tr><td colspan='3'><h2>General</h2></td></tr>");
        writer.println("<tr><td>Name: </td><td colspan='2' ><input name='name' type='text' value='" + name + "' /></td></tr>");
        writer.println("<tr><td>Starred: </td><td colspan='2' ><input name='starred' type='checkbox' " + (starred?"checked='checked'":"") + " /></td></tr>");
        writer.println("<tr><td>Send to Voicemail: </td><td colspan='2'><input name='voicemail' type='checkbox' " + (voicemail?"checked='checked'":"")
                + " /><td></tr>");
        writer.println("<tr><td>Notes: </td><td colspan='2'  ><textarea name='notes'>" + (notes != null?notes:"") + "</textarea></td></tr>");
        writer.println("<tr><td colspan='2'><a href='/console/contacts/html/" + id + "/photo'><img src=\"/console/contacts/html/" + id + "/photo\""
                + "/></a></td><td><input type='file' name='new-pic'>Change photo</input></td>");
        writer.println("<tr><td colspan='3'><h2>Phone numbers</h2></td></tr>");
        Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(),id);
        if (phones != null)
        {
            ContentValues phone;
            while ((phone = phones.next()) != null)
            {
                String phoneId = phone.getAsString(android.provider.BaseColumns._ID);
                String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
                String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);
                int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
                writer.println(createPhoneEditor(phoneId,label,type,number));
            }
        }
        //Put on a spare one so they can add another number
        writer.println(createPhoneEditor("x","",-1,""));

        writer.println("<tr><td colspan='3'><h2>Contact Methods</h2></td></tr>");
        ContactMethod.ContactMethodsCollection contactMethods = ContactMethod.getContactMethods(getContentResolver(),id);
        if (contactMethods != null)
        {
            ContentValues contactMethod;
            while ((contactMethod = contactMethods.next()) != null)
            {
                String contactMethodId = contactMethod.getAsString(android.provider.BaseColumns._ID);
                String label = contactMethod.getAsString(Contacts.ContactMethodsColumns.LABEL);
                String value = contactMethod.getAsString(Contacts.ContactMethodsColumns.DATA);
                int type = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.TYPE).intValue();
                int kind = contactMethod.getAsInteger(Contacts.ContactMethodsColumns.KIND).intValue();
                writer.println(createContactMethodEditor(contactMethodId,type,kind,label,value));
            }
        }
        //Put on a spare one so they can add another
        writer.println(createContactMethodEditor("x",-1,-1,"",""));

        writer.println("</table>");
        writer.println("<br /><input type='submit' name='save' id='save' value='Save' /> <a href='/console/contacts/html/" + (id != null?id.toString():"")
                + "'><button id='cancel'>Cancel</button></a></form>");
        writer.println("</div>");
    }

    /**
     * doGetUser
     *
     * Output some information on a particular User, such as phones, email addresses etc.
     *
     * @param writer
     * @param request
     * @param response
     * @param who
     * @throws ServletException
     * @throws IOException
     */
    protected void doGetUser(PrintWriter writer, HttpServletRequest request, HttpServletResponse response, String who) throws ServletException, IOException
    {
        //query for the user's standard details
        ContentValues values = User.get(getContentResolver(),who);
        formatSummaryUserDetails(values,writer);

        //query for all phone details
        Phone.PhoneCollection phones = Phone.getPhones(getContentResolver(),who);
        formatPhones(who,phones,writer);
        phones.close();

        //query for all contact details
        ContactMethod.ContactMethodsCollection contactMethods = ContactMethod.getContactMethods(getContentResolver(),who);
        formatContactMethods(who,contactMethods,writer);
        contactMethods.close();

        writer.println("<br /><a target='_top' href='/console/contacts/html/" + who + "?action=" + __ACTION_EDIT
                + "'><button id='edit'>Edit</button></a>&nbsp;<a target='_top' href=\"/console/contacts/html/" + who + "?action=" + __ACTION_DEL
                + "\"><button id='del'>Delete</button></a>");
    }

    /**
     * doGetUsers
     *
     * Output an overview page of all the Users.
     *
     * @param writer
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGetUsers(PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // This HTML is used for the side pane for desktop browsers that use Javascript.
        writer.println("<h1 id='pg-head' class='pageheader'>Contact List</h1>");
        // Print out the table that everyone can read.
        User.UserCollection users = User.getAll(getContentResolver());

        formatUserDetails(users,writer);
        users.close();

        writer.println("<br /><a href=\"/console/contacts/html/?action=" + __ACTION_ADD + "\"><button id='add'>Add</button></a>");
        writer.println("</div>");
    }

    private void formatContactMethods(String who, ContactMethod.ContactMethodsCollection contactMethods, PrintWriter writer)
    {
        writer.println("<h2>Addresses</h2>");
        writer.println("<table id='addresses' style='border: 0px none;'>");
        int row = 0;
        ContentValues contactMethod;

        while ((contactMethod = contactMethods.next()) != null)
        {
            String style = HTMLHelper.getRowStyle(row);
            writer.println("<tr class='" + style + "'>");
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
                    typeStr = __LABELS.get(_contactTypes.get(Integer.valueOf(type)));
                }
            }

            String kindStr = __LABELS.get(_contactKinds.get(Integer.valueOf(kind)));
            switch (kind)
            {
                case Contacts.KIND_EMAIL:
                {

                    printCell(writer,"<span class='label'>" + kindStr + ":</span>",style);
                    printCell(writer,"<a href=\"mailto:" + data + "\">" + data + "</a>",style);
                    printCell(writer,typeStr,style);
                    break;
                }
                case Contacts.KIND_IM:
                {
                    printCell(writer,"<span class='label'>" + kindStr + ":</span>",style);
                    printCell(writer,data,style);
                    printCell(writer,typeStr,style);
                    break;
                }
                case Contacts.KIND_POSTAL:
                {
                    printCell(writer,"<span class='label'>" + kindStr + ":</span>",style);
                    printCell(writer,data,style);
                    printCell(writer,typeStr,style);
                    break;
                }
                default:
                {
                    printCell(writer,kindStr,style);
                    if (data != null)
                    {
                        printCell(writer,data,style);
                    }
                    if (auxData != null)
                    {
                        printCell(writer,data,style);
                    }

                    printCell(writer,typeStr,style);
                    break;
                }
            }
            writer.println("</tr>");
            row++;
        }

        writer.println("</table>");
    }

    private void formatPhones(String who, Phone.PhoneCollection phones, PrintWriter writer)
    {
        writer.println("<h2>Phone Numbers</h2>");
        writer.println("<table id='phones' style='border: 0px none;'>");
        int row = 0;
        ContentValues phone;
        while ((phone = phones.next()) != null)
        {
            String style = HTMLHelper.getRowStyle(row);
            writer.println("<tr class='" + style + "'>");

            String label = phone.getAsString(Contacts.PhonesColumns.LABEL);
            String number = phone.getAsString(Contacts.PhonesColumns.NUMBER);

            int type = phone.getAsInteger(Contacts.PhonesColumns.TYPE).intValue();
            String phoneType = __LABELS.get(_phoneTypes.get(Integer.valueOf(type)));
            printCell(writer,phoneType,style);

            String encodedNumber = number;

            try
            {
                encodedNumber = URLEncoder.encode(number,"UTF-8");
            }
            catch (Exception e)
            {
                Log.w(TAG,"Encoding telephone number failed");
            }

            printCell(writer,(number == null?"Unknown":"<a href=\"/console/contacts/html/" + who + "?action=" + __ACTION_CALL + "&number=" + encodedNumber
                    + "\">" + number + "</a>" + (label == null?"":"&nbsp;<span class='phone-label'>(" + label + ")</span>")),style);
            writer.println("</tr>");

            row++;
        }
        writer.println("</table>");
    }

    /**
     * formtaSummaryUserDetails
     *
     * For a given user, write out all the info we know about them.
     *
     * @param values
     * @param writer
     */
    private void formatSummaryUserDetails(ContentValues values, PrintWriter writer)
    {
        if ((values != null) && (writer != null))
        {
            String id = values.getAsString(android.provider.BaseColumns._ID);
            String name = values.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
            String title = null;
            String company = null;
            String notes = values.getAsString(Contacts.PeopleColumns.NOTES);
            Integer i = values.getAsInteger(Contacts.PeopleColumns.STARRED);
            boolean starred = (i == null?false:i.intValue() > 0);
            i = values.getAsInteger(Contacts.PeopleColumns.SEND_TO_VOICEMAIL);
            boolean voicemail = (i == null?false:i.intValue() > 0);

            writer.println("<h1 class='pageheader'>" + (starred?"<span class='big'>*</span>&nbsp;":"") + (title == null?"":title + "&nbsp;")
                    + (name == null?"Unknown":name) + "</h1>");

            writer.println("<div id='content'>");
            writer.println("<h2>Photo</h2><a href='/console/contacts/html/" + id + "/photo'><img src=\"/console/contacts/html/" + id + "/photo\"" + "/></a>");
            if (company != null)
            {
                writer.println("<p>Company: " + company + "</h3></p>");
            }
            if (voicemail)
            {
                writer.println("<p>Goes to Voicemail</p>");
            }
            writer.println("<h2>Notes</h2>");
            writer.println("<table id='notes' style='border: 0px none;'>");
            writer.println("<tr>");
            writer.println("<td>");
            if (notes != null)
            {
                writer.println(notes);
            }
            else
            {
                writer.println("&nbsp;");
            }
            writer.println("</td>");
            writer.println("</tr>");
            writer.println("</table>");
        }
    }

    /**
     * formatUserDetails
     *
     * For a set of users, print out a 1 line of data.
     *
     * @param users
     * @param writer
     */
    private void formatUserDetails(User.UserCollection users, PrintWriter writer)
    {
        if ((users != null) && (writer != null))
        {
            writer.println("<table id='user'>");
            writer.println("<thead><tr>");
            writer.println("<th>Starred</th><th>Photo</th><th>Name</th>");
            writer.println("</tr></thead><tbody>");

            int row = 0;
            ContentValues user = null;
            while ((user = users.next()) != null)
            {
                String style = HTMLHelper.getRowStyle(row);

                String id = user.getAsString(android.provider.BaseColumns._ID);
                writer.println("<tr" + style + " id='contact-" + id + "'>");
                String name = user.getAsString(Contacts.PeopleColumns.DISPLAY_NAME);
                String title = null;
                String company = null;
                String notes = user.getAsString(Contacts.PeopleColumns.NOTES);
                Integer i = user.getAsInteger(Contacts.PeopleColumns.STARRED);

                boolean starred = (i == null?false:i.intValue() > 0);
                printCell(writer,(starred?"<span class='big'>*</span>":"&nbsp;"),style);
                printCell(writer,"<a class='userlink' href='/console/contacts/html/" + id + "/'><img src=\"/console/contacts/html/" + id + "/photo\""
                        + " /></a>",style);
                printCell(writer,"<a class='userlink' href=\"/console/contacts/html/" + id + "\">" + name + "</a>",style);
                writer.println("</tr>");
                ++row;
            }
            writer.println("</tbody></table>");

            if (row == 0)
            {
                writer.println("<h2 style='text-align: center;'>Sorry, you haven't added any contacts to your phone!</h2>");
            }
        }
    }

    @Override
    public void handleAddUser(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        PrintWriter writer = response.getWriter();
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        HTMLHelper.doHeader(writer,request,response);
        HTMLHelper.doMenuBar(writer,request,response);
        doEditUser(writer,request,response,null);
        HTMLHelper.doFooter(writer,request,response);
    }

    @Override
    public void handleDefault(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        handleGetUsers(request,response);
    }

    @Override
    public void handleDeleteUser(HttpServletRequest request, HttpServletResponse response, String who) throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        HTMLHelper.doHeader(writer,request,response);
        HTMLHelper.doMenuBar(writer,request,response);
        doDeleteUser(writer,request,response,who);
        doGetUsers(writer,request,response);
        HTMLHelper.doFooter(writer,request,response);
    }

    @Override
    public void handleEditUser(HttpServletRequest request, HttpServletResponse response, String who) throws IOException, ServletException
    {
        PrintWriter writer = response.getWriter();
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        HTMLHelper.doHeader(writer,request,response);
        HTMLHelper.doMenuBar(writer,request,response);
        doEditUser(writer,request,response,who);
        HTMLHelper.doFooter(writer,request,response);
    }

    @Override
    public void handleGetUser(HttpServletRequest request, HttpServletResponse response, String who) throws IOException, ServletException
    {

        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        doGetUser(writer,request,response,who);
        writer.write("\r\n");
        writer.close();

    }

    @Override
    public void handleGetUsers(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        PrintWriter writer = response.getWriter();
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        User.UserCollection users = User.getAll(getContentResolver());
        doGetUsers(writer,request,response);
        users.close();
    }

    @Override
    public void handleSaveUser(HttpServletRequest request, HttpServletResponse response, String who) throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        HTMLHelper.doHeader(writer,request,response);
        HTMLHelper.doMenuBar(writer,request,response);
        saveUserFormData(request,response,request.getParameter("id"));
        HTMLHelper.doFooter(writer,request,response);
    }

    private void printCell(PrintWriter writer, String cellContent, String cellStyle)
    {
        writer.println("<td" + cellStyle + ">");
        writer.println(cellContent);
        writer.println("</td>");
    }

    private void printCellHeader(PrintWriter writer, String cellContent, String cellStyle)
    {
        writer.println("<th>" + cellContent + "</th>");
    }
}
