package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IndexServlet extends InfoServlet
{

    @Override
    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<h1>Options</h1>");
        writer.println("<ul>");
        writer.println("<li><a href='/app/contacts'>Contacts</a></li>");
        writer.println("<li><a href='/app/settings'>System Settings</a></li>");
        writer.println("<li><a href='/app/calls'>Call Log</a></li>");
        writer.println("</ul>");
    }

}
