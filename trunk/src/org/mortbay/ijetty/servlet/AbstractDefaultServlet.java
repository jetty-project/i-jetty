package org.mortbay.ijetty.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.WriterOutputStream;
import org.mortbay.ijetty.R;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaderValues;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.servlet.Dispatcher;
import org.mortbay.util.IO;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;

import android.util.DateFormat;
import android.util.Log;
import android.content.Resources;


public class AbstractDefaultServlet extends HttpServlet
{

    private Resources _resources;
    
    
    public static abstract class AndroidResource
    {
        public abstract boolean exists();
        public abstract boolean isDirectory();
        public abstract InputStream getInputStream() throws java.io.IOException;
        public abstract OutputStream getOutputStream() throws java.io.IOException, SecurityException;
        
        public void writeTo (OutputStream out)
        throws IOException
        {
            InputStream is = getInputStream();
            IO.copy(is, out);
        }
        
        public static AndroidResource getResource (String path)
        {
            Log.i("Jetty", "Getting resource for path="+path);
            if (path.startsWith("contacts"))
            {
                //return new AndroidContactResource
                return null;
            }
            else if (path.startsWith("settings"))
            {
                //return new AndroidSettingsResource
                return null;
            }
            else 
                return new AndroidFileResource(path);
        }
        
    }
    
    public static class AndroidFileResource extends AndroidResource
    {
        private File _file;
        
        public AndroidFileResource (String file)
        {
            try
            {
                _file = new File("/sdata"+(file.startsWith("/")?"":"/")+file);
               
            }
            catch (Exception e)
            {
                Log.e("Jetty","Problem getting AndroidFileResource", e);
            }
        }
        
        public  boolean exists()
        {
            return _file.exists();
        }
        
        public  boolean isDirectory()
        {
            return _file.isDirectory();
        }
        public  long lastModified()
        {
            return _file.lastModified();
        }
        public  String[] list()
        {
            String[] list =_file.list();
            if (list==null)
                return null;
            for (int i=list.length;i-->0;)
            {
                if (new File(_file,list[i]).isDirectory() &&
                    !list[i].endsWith("/"))
                    list[i]+="/";
            }
            return list; 
        }
        
        public long length ()
        {
            return _file.length();
        }
        
        public AndroidFileResource addPath (String path)
        {
            
            if (!isDirectory())
            {
                String cp = URIUtil.canonicalPath(path);
                return new AndroidFileResource(URIUtil.addPaths(_file.getAbsolutePath(),cp));
            }
            else
            {
                String rel=path;
                if (path.startsWith("/"))
                    rel = path.substring(1);
                
                return new AndroidFileResource(URIUtil.addPaths(_file.getAbsolutePath(), URIUtil.encodePath(rel)));
            }
        }
        
        
        
        public String getListHTML(String base, boolean parent)
        {
            if (!isDirectory())
                return null;
            
            String[] ls = list();
            if (ls==null)
                return null;
            Arrays.sort(ls);
            
            String decodedBase = URIUtil.decodePath(base);
            String title = "Directory: "+decodedBase;

            StringBuffer buf=new StringBuffer(4096);
            buf.append("<HTML><HEAD><TITLE>");
            buf.append(title);
            buf.append("</TITLE></HEAD><BODY>\n<H1>");
            buf.append(title);
            buf.append("</H1><TABLE BORDER=0>");
            
            if (parent)
            {
                buf.append("<TR><TD><A HREF=");
                buf.append(URIUtil.addPaths(base,"../"));
                buf.append(">Parent Directory</A></TD><TD></TD><TD></TD></TR>\n");
            }
            
            
            for (int i=0 ; i< ls.length ; i++)
            {
                String encoded=URIUtil.encodePath(ls[i]);
                AndroidFileResource item = addPath(ls[i]);

                buf.append("<TR><TD><A HREF=\"");
                String path=URIUtil.addPaths(base,encoded);

                if (item.isDirectory() && !path.endsWith("/"))
                    path=URIUtil.addPaths(path,URIUtil.SLASH);
                buf.append(path);
                buf.append("\">");
                buf.append(StringUtil.replace(StringUtil.replace(ls[i],"<","&lt;"),">","&gt;"));
                buf.append("&nbsp;");
                buf.append("</TD><TD ALIGN=right>");
                buf.append(item.length());
                buf.append(" bytes&nbsp;</TD><TD>");
                buf.append(DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date(item.lastModified())));
                buf.append("</TD></TR>\n");
            }
            buf.append("</TABLE>\n");
            buf.append("</BODY></HTML>\n");

            return buf.toString();
        }


        public InputStream getInputStream() throws IOException
        {
            return new FileInputStream(_file);
        }


        public OutputStream getOutputStream() throws IOException,
                SecurityException
        {
           return new FileOutputStream(_file);
        }
 
    }
    
    
    
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
        try{out = ar.getOutputStream();}
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
