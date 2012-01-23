package org.mortbay.ijetty.handler;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ByteArrayISO8859Writer;
import org.eclipse.jetty.util.StringUtil;


public class DefaultHandler extends org.eclipse.jetty.server.handler.DefaultHandler
{

    public DefaultHandler()
    {
       super();
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        if (response.isCommitted() || baseRequest.isHandled())
            return;

        baseRequest.setHandled(true);

        String method=request.getMethod();

    

        if (!method.equals(HttpMethods.GET) || !request.getRequestURI().equals("/"))
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return; 
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(MimeTypes.TEXT_HTML);

        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer(1500);

        String uri=request.getRequestURI();
        uri=StringUtil.replace(uri,"<","&lt;");
        uri=StringUtil.replace(uri,">","&gt;");

        writer.write("<HTML>\n<HEAD>\n<TITLE>Welcome to i-jetty");
        writer.write("</TITLE>\n<BODY>\n<H2>Welcome to i-jetty</H2>\n");
        writer.write("<p>i-jetty is running successfully.</p>");

        Server server = getServer();
        Handler[] handlers = server==null?null:server.getChildHandlersByClass(ContextHandler.class);

        int i=0;
        for (;handlers!=null && i<handlers.length;i++)
        {
            if (i == 0)
                writer.write("<p>Available contexts are: </p><ul>");

            ContextHandler context = (ContextHandler)handlers[i];
            if (context.isRunning())
            {
                writer.write("<li><a href=\"");
                if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
                    writer.write("http://"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
                writer.write(context.getContextPath());
                if (context.getContextPath().length()>1 && context.getContextPath().endsWith("/"))
                    writer.write("/");
                writer.write("\">");
                writer.write(context.getContextPath());
                if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
                    writer.write("&nbsp;@&nbsp;"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
                writer.write("&nbsp;--->&nbsp;");
                writer.write(context.toString());
                writer.write("</a></li>\n");
            }
            else
            {
                writer.write("<li>");
                writer.write(context.getContextPath());
                if (context.getVirtualHosts()!=null && context.getVirtualHosts().length>0)
                    writer.write("&nbsp;@&nbsp;"+context.getVirtualHosts()[0]+":"+request.getLocalPort());
                writer.write("&nbsp;--->&nbsp;");
                writer.write(context.toString());
                if (context.isFailed())
                    writer.write(" [failed]");
                if (context.isStopped())
                    writer.write(" [stopped]");
                writer.write("</li>\n");
            }
            
            if (i == handlers.length -1)
                writer.write("</ul>\n");
        }
        
        if (i == 0)
            writer.write("<p>There are currently no apps deployed.</p>");

        for (int j=0;j<10;j++)
            writer.write("\n<!-- Padding for IE                  -->");

        writer.write("\n</BODY>\n</HTML>\n");
        writer.flush();
        response.setContentLength(writer.size());
        OutputStream out=response.getOutputStream();
        writer.writeTo(out);
        out.close();
    }

    
}
