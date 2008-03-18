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
    	writer.println("html {font-size:10pt;font-family:verdana, arial, helvetica, sans-serif;}");
        writer.println("table {border-width:0px;}");
        
        writer.println("h1 img {border:0px;}");
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
        writer.println(".big {font-size:18pt;}");
        writer.println(".label {font-variant:small-caps;font-weight:bold;}");
        writer.println(".promo {font-style:italic;font-size:8pt;background-color:#000099;color:#ffffff;}");
        writer.println(".promo a:link,.promo a:visited,.promo a:hover,.promo a:active {color:#ff6600;font-weight:bold;}");
    }

}
