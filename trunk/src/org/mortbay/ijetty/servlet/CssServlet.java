package org.mortbay.ijetty.servlet;

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
    	writer.println("html {font-size:10pt;font-family:sans-serif;}");
        writer.println("table {border-width:0px;border-style:groove;border-color:#aaaaaa;}");
        writer.println("h1 img {border:2px ridge #999999;}");
        writer.println("#navbar td {background-color:#000099;padding:4px;margin:4px;}");
        writer.println("#navbar td.sel {background-color:#0000ff;}");
        writer.println("#navbar a {font-weight:bold;font-variant:small-caps;text-align:center;text-decoration:none}");
        writer.println("#navbar a:link {color:#ffffff;}");        
        writer.println("#navbar a:active {color:#ff6600;}");
        writer.println("#navbar a:visited {color:#ffffff;}");
        writer.println("#navbar a:hover {color:#ff6600;}");
        writer.println("a img {border:0px}");
        writer.println("th {font-variant:small-caps;text-align:left;}");
        writer.println(".odd {background:#efefef;}");
        writer.println(".even {background:#ffffff;}");
        writer.println(".primary {font-weight:bold;}");
        writer.println(".big {font-size:18pt;}");
        writer.println(".label {font-variant:small-caps;font-weight:bold}");
    }

}
