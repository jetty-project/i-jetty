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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentResolver;

public class IPServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private ContentResolver resolver;

    protected void doContent(PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {

        writer.println("<h1 class='pageheader'>Network Interfaces Reported by Android</h1><div id='content'>");
        try
        {
            writer.println("<table cellspacing='0' cellpadding='2' border='1'>");
            
            writer.println("<thead><th>Interface</th><th>IP</th></thead>");
            writer.println("<tbody>");
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis))
            {
                writer.println("<tr>");
                writer.printf("<td valign='top'>%s</td>%n",ni.getDisplayName());
                writer.println("<td>");
                Enumeration<InetAddress> iis = ni.getInetAddresses();
                for (InetAddress ia : Collections.list(iis))
                {
                    writer.printf("<p class='singleline'>%s</p>",ia.getHostAddress());
                }
                writer.println("</td>");
                writer.println("</tr>");
            }
            writer.println("</tbody>");
            writer.println("</table>");
        }
        catch (SocketException e)
        {
            writer.println("Socket Exception: No Network Interfaces Available?");
        }
        writer.println("</div>");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        HTMLHelper.doHeader(writer,request,response);
        HTMLHelper.doMenuBar(writer,request,response);
        doContent(writer,request,response);
        HTMLHelper.doFooter(writer,request,response);
    }

    public ContentResolver getContentResolver()
    {
        return resolver;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
    }

}
