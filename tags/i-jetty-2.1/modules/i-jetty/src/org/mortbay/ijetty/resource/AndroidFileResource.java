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


package org.mortbay.ijetty.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;

import org.mortbay.log.Log;

public class AndroidFileResource extends AndroidResource
{
    private File _file;
    
   
    public AndroidFileResource (URL url)
    {
    	try
    	{    		
    		_file = new File (new URI(url.toString()));
    		if (Log.isDebugEnabled()) Log.debug("Made AndroidFileResource for "+ _file.getCanonicalPath());
    	}
    	catch (Exception e)
    	{
    		Log.warn("Problem getting AndroidFileResource", e);
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
    throws MalformedURLException
    {
        
        if (!isDirectory())
        {
            String cp = URIUtil.canonicalPath(path);
            return new AndroidFileResource(new URL(URIUtil.addPaths(_file.toURL().toExternalForm(), cp)));
        }
        else
        {
            String rel=URIUtil.canonicalPath(path);
            if (rel.startsWith("/"))
                rel = path.substring(1);
            
            return new AndroidFileResource(new URL(URIUtil.addPaths(_file.toURL().toString(), URIUtil.encodePath(rel))));
        }
    }
    
    
    
    public String getListHTML(String base, boolean parent)
    throws IOException
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
            // FIXME: is toString sufficient for our formatting needs?
            buf.append(new Date(item.lastModified()).toString());
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
            Log.warn("Problem getting name of file ",e);
            return "AndroidFileResource:unknown";
        }
    }
}
