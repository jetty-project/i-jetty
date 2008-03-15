package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CssServlet extends InfoServlet
{

    @Override
    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("table {border-width:3px;border-style:groove;border-color:#aaaaaa;}");
        writer.println("#navbar td {border:0;background-color:#000088;padding:4px;}");
        writer.println("#navbar td.sel {border:0;background-color:#0000ff;}");
        writer.println("#navbar a:link {color:#ffffff;font-weight:bold;font-variant:small-caps;text-align:center;}");        
        writer.println("#navbar a:active {color:#ff0000;font-weight:bold;font-variant:small-caps;text-align:center;}");
        writer.println("#navbar a:visited {color:#ffffff;font-weight:bold;font-variant:small-caps;text-align:center;}");
        writer.println("th {font-variant:small-caps;text-align:left;}");
        writer.println(".odd {background:#efefef;border:solid #aaaaaa 1px;}");
        writer.println(".even {background:#ffffff;border:solid #aaaaaa 1px;}");
    }

}
