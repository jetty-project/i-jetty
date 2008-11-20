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
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.URIUtil;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;

public abstract class InfoServlet extends HttpServlet
{
    private String[] _navBarLabels = {"Contacts", "System Settings", "Call Logs", "Network"};
    private String[] _navBarItems = {"/console/contacts", "/console/settings","/console/calls", "/console/network"};
    private String[] _phrases = { "Now with 100% more awesome.", "Better than cake before dinner!", "Chuck Norris approves.", "werkin teh intarwebz sinse 1841", "It's lemon-y fresh!", "More amazing than a potato.", "All the cool kids are doing it!", "Open sauce, eh?", "<code>Nothing happens.</code>", "I told you we should've taken a left!" };

    public ContentResolver getContentResolver ()
    {
        return (ContentResolver)getServletContext().getAttribute("contentResolver");
    }
    protected void doHeader (PrintWriter writer,HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        writer.println("<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>");
        writer.println("<head>");
        writer.println("    <title>i-jetty Console</title>");
        writer.println("    <link rel='stylesheet' type='text/css' media='screen' href='/console/console.css' />");
        writer.println("    <meta name='viewport' content='width=device-width,minimum-scale=1.0,maximum-scale=1.0'/>");
        writer.println("</head>");
        writer.println("<body>");
    }
    
    protected void doFooter (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Random generator = new Random();
        
        writer.println("    </div>");
        writer.println("    <div id='footer'>");
        writer.println("        Served up by Jetty.<br />" + _phrases[generator.nextInt(_phrases.length)]);
        writer.println("    </div>");
        writer.println("</body>");
        writer.println("</html>");
    }
    
    protected void doMenuBar (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("    <div id='navigation'><ul>");
        String path = request.getServletPath();
        
        for (int i=0; i<_navBarItems.length; i++)
        {
            writer.print("        <li>");
            
            String[] splitPath = _navBarItems[i].split("/");
            if (path.endsWith(splitPath[splitPath.length - 1]))
                writer.print("<strong>"+_navBarLabels[i]+"</strong>");
            else
                writer.print("<a href='"+_navBarItems[i]+"'>"+_navBarLabels[i]+"</a>");
            
            writer.println("</li>");
        }
        writer.println("    </ul></div>");
        writer.println("    <div id='page' style='min-height: 400px;'>");
    }
    
    protected abstract void doContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
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
            writer.println("<table>");
            writer.println("<tr>");
            for (int i=0;i<colNames.length;i++)
                writer.println("    <th>"+colNames[i]+"</th>");
            writer.println("</tr>");
            int row = 0;
            while (cursor.moveToNext())
            {  
                String classExtra = getRowStyle(row);
                writer.println("<tr>");
                for (int i=0;i<colNames.length;i++)
                {
                    String val=cursor.getString(i);
                    writer.println("<td"+classExtra+">"+(val==null?"&nbsp;":val)+"</td>");
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
            return "";
        else
            return " class='odd'";

    }
    
    protected boolean isMobileClient (HttpServletRequest request)
    {
        String useragent = request.getHeader("User-Agent");
        if (useragent != null && (useragent.contains("Android") || useragent.contains("iPhone")))
        {
            return true;
        }
        
        return false;
    }
}
