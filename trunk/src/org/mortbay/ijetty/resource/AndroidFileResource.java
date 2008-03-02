package org.mortbay.ijetty.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;

import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;

import android.util.DateFormat;
import android.util.Log;

public class AndroidFileResource extends AndroidResource
{
    private File _file;
    
    public AndroidFileResource (String file)
    {
        try
        {
            _file = new File("/sdcard"+(file.startsWith("/")?"":"/")+file);
            Log.d("Jetty", "Made AndroidFileResource for "+_file.getCanonicalPath());
           
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
        Log.d("Jetty", "Last modified time for "+_file.getName()+_file.lastModified());
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

    public String toString ()
    {
        try
        {
            return _file.getCanonicalPath();
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Problem getting name of file ",e);
            return "AndroidFileResource:unknown";
        }
    }
}
