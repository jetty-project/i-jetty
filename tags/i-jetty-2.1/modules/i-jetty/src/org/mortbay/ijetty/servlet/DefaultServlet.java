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

package org.mortbay.ijetty.servlet;


import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.resource.AndroidFileResource;
import org.mortbay.io.WriterOutputStream;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.Dispatcher;
import org.mortbay.util.URIUtil;

import android.content.res.Resources;



public class DefaultServlet extends HttpServlet
{
    private String[] _welcomes =  {"index.html", "index.htm"};   
    private boolean _redirectWelcome=false;
    private boolean _dirAllowed=true;
    private ContextHandler.SContext _context;
    private Resources _resources;
    
    
    public DefaultServlet ()
    {}
    
    
    public void init()
    {
       String tmp = getInitParameter("redirectWelcome");
       if (tmp!=null)
           _redirectWelcome=Boolean.valueOf(tmp.trim());
       tmp = getInitParameter("dirAllowed");
       if (tmp!=null)
           _dirAllowed=Boolean.valueOf(tmp.trim());
       ServletContext config=getServletContext();
       _context = (ContextHandler.SContext)config;
    }
    
    public AndroidFileResource getResource (URL url)
    {
    	if (url == null)
    		return null;
    	
    	return new AndroidFileResource(url);
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
        
        URL url = getServletContext().getResource(pathInContext);
        AndroidFileResource androidFileResource = getResource (url);
        
        if (androidFileResource == null)
        {
        	response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        else if (androidFileResource.isDirectory() && !endsWithSlash)
        {
            //if url has no final /, then add trailing / and redirect
                StringBuffer buf=request.getRequestURL();
                int param=buf.lastIndexOf(";");
                if (param<0)
                    buf.append('/');
                else
                    buf.insert(param,'/');
                String q=request.getQueryString();
                if (q!=null&&q.length()!=0)
                {
                    buf.append('?');
                    buf.append(q);
                }
                response.setContentLength(0);
                response.sendRedirect(response.encodeRedirectURL(buf.toString()));
        }
        else if (androidFileResource.isDirectory() && endsWithSlash)
        {
            //check for welcome file
            String welcome=getWelcomeFile(androidFileResource);
            if (welcome!=null)
            {
                String ipath=URIUtil.addPaths(pathInContext,welcome);
                if (_redirectWelcome)
                {
                    // Redirect to the index
                    response.setContentLength(0);
                    String q=request.getQueryString();
                    if (q!=null&&q.length()!=0)
                        response.sendRedirect(URIUtil.addPaths( _context.getContextPath(),ipath)+"?"+q);
                    else
                        response.sendRedirect(URIUtil.addPaths( _context.getContextPath(),ipath));
                }
                else
                {
                    // Forward to the index
                    RequestDispatcher dispatcher=request.getRequestDispatcher(ipath);
                    if (dispatcher!=null)
                    {
                        if (included.booleanValue())
                            dispatcher.include(request,response);
                        else
                        {
                            request.setAttribute("org.mortbay.jetty.welcome",ipath);
                            dispatcher.forward(request,response);
                        }
                    }
                }
            }
            else
            {
                //else do a directory listing    
                if (included.booleanValue() || passConditionalHeaders(request,response, androidFileResource))
                    sendDirectory(request,response,androidFileResource,pathInContext.length()>1);
            }
        }
        else
        {
            if (androidFileResource.exists())
            {
                if (included.booleanValue() || passConditionalHeaders(request,response, androidFileResource))  
                {
                    sendData(request,response,included.booleanValue(),androidFileResource);  
                }
            }
            else
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }   
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
    
    
    /* Check modification date headers.
     */
    protected boolean passConditionalHeaders(HttpServletRequest request,HttpServletResponse response, AndroidFileResource androidResource)
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
            AndroidFileResource afr)
    throws IOException
    {
        // Get the output stream (or writer)
        OutputStream out =null;
        try{out = response.getOutputStream();}
        catch(IllegalStateException e) {out = new WriterOutputStream(response.getWriter());}
        
        if (include)
        {
            afr.writeTo(out);
        }
        else
        {
            // Write content normally
            writeHeaders(response,afr);
            afr.writeTo(out);
        }
    }

    protected void writeHeaders(HttpServletResponse response, AndroidFileResource afr)
    throws IOException
    {

        if (afr==null)
            return;
    
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
    
    private String getWelcomeFile(AndroidFileResource dir) throws IOException
    {
        if (!dir.isDirectory() || _welcomes==null)
            return null;

        for (int i=0;i<_welcomes.length;i++)
        {
            AndroidFileResource welcome=dir.addPath(_welcomes[i]);
            if (welcome.exists())
                return _welcomes[i];
        }

        return null;
    }
    
    public void setResources (Resources resources)
    {
    	_resources = resources;
    }

}
