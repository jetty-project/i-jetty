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
        writer.println("th {font-variant:small-caps;text-align:left;}");
        writer.println(".odd {background:#efefef;border:solid #aaaaaa 1px;}");
        writer.println(".even {background:#ffffff;border:solid #aaaaaa 1px;}");
    }

}
