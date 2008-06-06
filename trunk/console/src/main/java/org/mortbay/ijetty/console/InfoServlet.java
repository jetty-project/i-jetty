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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.util.URIUtil;

import android.content.ContentResolver;
import android.database.Cursor;

public abstract class InfoServlet extends HttpServlet
{
    private String[] _navBarLabels = {"Contacts","Call Log", "Settings"};
    private String[] _navBarItems = {"/app/contacts/", "/app/calls/","/app/settings/"};

    public ContentResolver getContentResolver ()
    {
        return (ContentResolver)getServletContext().getAttribute("contentResolver");
    }
    protected void doHeader (PrintWriter writer,HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<html>");
        writer.println("<head><META http-equiv=\"Pragma\" content=\"no-cache\"> <META http-equiv=\"Cache-Control\" content=\"no-cache,no-store\">");
        writer.println(" <link rel=\"stylesheet\" type=\"text/css\" href=\"/app/css\"></link></head>");
        writer.println("<body>");
        writer.println("<table>");
        writer.println("<tr><td>");
    }
    
    protected void doFooter (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	writer.println("</td></tr>");
    	writer.println("<tr><td class='promo'>");
    	writer.println("<span class='promo'>This page served by <a href='http://jetty.mortbay.org'>Jetty</a></span>");
    	writer.println("</td></tr>");
    	writer.println("</table>");
    	writer.println("</body>");
        writer.println("</html>");
    }
    
    protected void doMenuBar (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<table width='100%'><tr><td><ul id='ulnavbar'>");
        for (int i=0; i<_navBarItems.length; i++)
        {
            String pathInContext=URIUtil.addPaths(request.getServletPath(),request.getPathInfo());
            if (pathInContext.startsWith(_navBarItems[i]))
                writer.println("<li class='sel'>");
            else
                writer.println("<li>");
            
            writer.println("<a href='"+_navBarItems[i]+"'>"+_navBarLabels[i]+"</a>");
            writer.println("</li>");
        }
        writer.println("</td></tr></table>");
    }
    
    protected abstract void doContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        base_request.setHandled(true);

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        doHeader(writer, request, response);
        doMenuBar(writer, request, response);
        doContent(writer, request, response);
        doFooter (writer, request, response);
    }

 
    protected void formatTable (String[] colNames, Cursor cursor, PrintWriter writer)
    {   
        if (colNames!=null && cursor!=null && writer!=null)
        {
            writer.println("<table class='generic'>");
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

                writer.println("<tr class='"+style+"'>");
                for (int i=0;i<colNames.length;i++)
                {
                    String val=cursor.getString(i);
                    writer.println("<td class=\""+style+"\">"+(val==null?"&nbsp;":val)+"</td>");
                }
                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");

        }
    }
    
    protected String getRowStyle (int row)
    {
        if (row%2==0)
            return "even";
        else
            return "odd";

    }
}
