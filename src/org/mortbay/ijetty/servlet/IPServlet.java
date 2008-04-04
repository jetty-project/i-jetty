package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IPServlet extends InfoServlet
{

	@Override
	protected void doContent(PrintWriter writer, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		
        writer.println("<h1>Phone Network Interfaces</h1>");
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
	}

}
