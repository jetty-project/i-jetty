package org.mortbay.ijetty.servlet;


import java.io.IOException;

import java.io.OutputStream;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.resource.AndroidFileResource;
import org.mortbay.ijetty.resource.AndroidResource;
import org.mortbay.io.WriterOutputStream;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.servlet.Dispatcher;

import org.mortbay.util.URIUtil;

import android.content.Resources;
import android.util.Log;


public class DefaultServlet extends HttpServlet
{

    private Resources _resources;
    
    
    
    public void setResources (Resources resources)
    {
        _resources=resources;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
        String servletPath=null;
        String pathInfo=null;
        Boolean included =(Boolean)request.getAttribute(Dispatcher.__INCLUDE_JETTY);
        
        if (included!=null && included.booleanValue())
        {
            servletPath=(String)request.getAttribute(Dispatcher.__INCLUDE_SERVLET_PATH);
            pathInfo=(String)request.getAttribute(Dispatcher.__INCLUDE_PATH_INFO);
            if (servletPath==null)
            {
                servletPath=request.getServletPath();
                pathInfo=request.getPathInfo();
            }
        }
        else
        {
            included=Boolean.FALSE;
            servletPath=request.getServletPath();
            pathInfo=request.getPathInfo();
        }
        
        String pathInContext=URIUtil.addPaths(servletPath,pathInfo);
        boolean endsWithSlash=pathInContext.endsWith(URIUtil.SLASH);
        AndroidResource ar = AndroidResource.getResource(pathInContext);
        
        if (ar.exists())
            Log.d("Jetty", "Exists AndroidFileResource "+ar.toString());
        else
            Log.d("Jetty", "Not exists AndroidFileResource "+ar.toString());
        
        Log.d("Jetty", "Length: "+((AndroidFileResource)ar).length());
       
        if (ar==null || !ar.exists())
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        else if (!ar.isDirectory())
        {   
            if (included.booleanValue() || passConditionalHeaders(request,response, ar))  
            {
                sendData(request,response,included.booleanValue(),ar);  
            }
        }
        else if (included.booleanValue() || passConditionalHeaders(request,response, ar))
            sendDirectory(request,response,(AndroidFileResource)ar,pathInContext.length()>1);
            
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        doGet(req,resp);
    }
    
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    /*
     * consider mapping urls like:
     * 
     * content/authority/path/id
     * ==
     * content://authority/path/id
     * eg
     * content://contacts/people/23
     * content/contacts/people/23
     * 
     * sdcard/this/is/my/xyz.file
     */
    
    /* Check modification date headers.
     */
    protected boolean passConditionalHeaders(HttpServletRequest request,HttpServletResponse response, AndroidResource androidResource)
    throws IOException
    {
        if (!request.getMethod().equals(HttpMethods.HEAD) )
        {
            String ifms=request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
            if (ifms!=null)
            {
                if (androidResource!=null)
                {
//                   Handle checking modification time on an android resource
//                            response.reset();
//                            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
//                            response.flushBuffer();
//                            return false;
                }
                    
                long ifmsl=request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
                if (ifmsl!=-1)
                {
                    /*
                    if (resource.lastModified()/1000 <= ifmsl/1000)
                    {
                        response.reset();
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        response.flushBuffer();
                        return false;
                    }
                    */
                }
            }

            // Parse the if[un]modified dates and compare to resource
            long date=request.getDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
            
            if (date!=-1)
            {
                /*
                if (resource.lastModified()/1000 > date/1000)
                {
                    response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }
                */
            }
            
        }
        return true;
    }
    

    protected void sendDirectory(HttpServletRequest request,
            HttpServletResponse response,
            AndroidFileResource resource,
            boolean parent)
    throws IOException
    {
        /*
        if (!_dirAllowed)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        */
        byte[] data=null;
        String base = URIUtil.addPaths(request.getRequestURI(),URIUtil.SLASH);
        String dir = resource.getListHTML(base,parent);
        if (dir==null)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "No directory");
            return;
        }

        data=dir.getBytes("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    
    protected void sendData (HttpServletRequest request,
            HttpServletResponse response,
            boolean include,
            AndroidResource ar)
    throws IOException
    {
        // Get the output stream (or writer)
        OutputStream out =null;
        try{out = response.getOutputStream();}
        catch(IllegalStateException e) {out = new WriterOutputStream(response.getWriter());}
        
        if (include)
        {
            ar.writeTo(out);
        }
        else
        {
            // Write content normally
            writeHeaders(response,ar);
            ar.writeTo(out);
        }
    }

    protected void writeHeaders(HttpServletResponse response, AndroidResource ar)
    throws IOException
    {

        if (!(ar instanceof AndroidFileResource))
            return;
        
        AndroidFileResource afr = (AndroidFileResource)ar;
        long lml=afr.lastModified();
        if (response instanceof Response)
        {
            Response r=(Response)response;
            HttpFields fields = r.getHttpFields();
            if (lml!=-1)
                fields.putDateField(HttpHeaders.LAST_MODIFIED_BUFFER,lml);
        }
        else
        {
            if (lml>0)
                response.setDateHeader(HttpHeaders.LAST_MODIFIED,lml);
        }
    }

}
