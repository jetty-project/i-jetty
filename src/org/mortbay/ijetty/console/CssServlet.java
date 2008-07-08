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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CssServlet extends HttpServlet
{

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/css");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.println("html {font-size:0.8em;font-family:helvetica, sans-serif;}");
        writer.println("table {border-width:0px;}");
        
        writer.println("h1 img {border:0px;}");
        writer.println("h1,h2,h3 {color: #000099;font-family:arial;}");
        writer.println("h1 {margin-top:0.2em;border-bottom: 1px dashed #000099;}");
        writer.println("#navbar td {background-color:#000099;padding:6px;margin:6px;}");
        writer.println("#navbar td.sel {background-color:#0000cc;}");
        writer.println("#navbar a {font-family:sans-serif;font-weight:bold;font-size:12pt;text-align:center;text-decoration:none}");
        writer.println("#navbar a:link {color:#ffffff;}");        
        writer.println("#navbar a:active {color:#ff6600;}");
        writer.println("#navbar a:visited {color:#ffffff;}");
        writer.println("#navbar a:hover {color:#ff6600;}");
        writer.println("a img {border:0px}");
        writer.println("th {font-variant:small-caps;text-align:left;}");
        writer.println(".odd {background:#efefef;}");
        writer.println(".even {background:#ffffff;}");
        writer.println(".primary {font-weight:bold;}");
        writer.println(".big {font-size:2em;}");
        writer.println(".label {font-variant:small-caps;font-weight:bold;}");
        writer.println(".promo {font-style:italic;font-size:0.7em;background-color:#000099;color:#ffffff;}");
        writer.println(".promo a:link,.promo a:visited,.promo a:hover,.promo a:active {color:#ff6600;font-weight:bold;}");
        writer.println("table #user, table #notes, table #phones, table #addresses, table .generic {border: 1px solid #efefef; border-collapse:collapse;}");
        writer.println("#user td, #notes td, #phones td, #addresses td {padding: 0.5em;}");
        writer.println("#addresses td {padding-left:0.5em; padding-right:0.5em;}");
        writer.println(".qualifier {font-style:italic;font-family:\"Times New Roman\"; font-size:0.8em;}");

        writer.println("#ulnavbar {color:#000000; border-bottom: 4px solid #000099; margin 0.5em 0em 0em 0em; padding:0em; padding-left:0.5em;z-index:1;}");
        writer.println("#ulnavbar li {display:inline; overflow:hidden;list-style-type:none;}");
        writer.println("#ulnavbar a {background-color:#000099; border:1px solid #000099;padding:0.5em 1em 0em 1em;;margin:0em;font-family:arial;font-weight:bold;font-size:0.8em;text-align:center;text-decoration:none}");
        writer.println("#ulnavbar a:link {color:#ffffff;}");
        writer.println("#ulnavbar a:active {color:#ffffff;}");
        writer.println("#ulnavbar a:visited {color:#ffffff;}");
        writer.println("#ulnavbar a:hover {background-color:#ff6600;}");
        writer.println("#ulnavbar a.sel {border-bottom: 5px solid  #ffffff; background-color:#ffffff; color:#000099;}");
        writer.println("button {color: #000099;}");
    }

}
