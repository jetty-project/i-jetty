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
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.console.InfoServlet;

public class IPServlet extends InfoServlet
{

        @Override
        protected void doContent(PrintWriter writer, HttpServletRequest request,
                        HttpServletResponse response) throws ServletException, IOException
        {
                
        writer.println("<h1>Network</h1><div id='content'>");
        Enumeration ni = NetworkInterface.getNetworkInterfaces();
        writer.println("<ul>");
        while (ni.hasMoreElements())
        {
                writer.println("<li>"+((java.net.NetworkInterface)ni.nextElement()).getName());
                Enumeration ii = ((java.net.NetworkInterface)ni.nextElement()).getInetAddresses();
                writer.println("<ul>");
                while (ii.hasMoreElements())
                {
                        InetAddress ia = ((InetAddress)ii.nextElement());
                        writer.println("<li>"+ia.getHostAddress()+"</li>");
                }  
                writer.println("</ul>");
                writer.println("</li>");
        }
        writer.println("</ul>");
        writer.println("</div>");
        }

}
