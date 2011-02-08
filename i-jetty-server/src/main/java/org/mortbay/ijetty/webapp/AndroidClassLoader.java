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

package org.mortbay.ijetty.webapp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import dalvik.system.DexClassLoader;


/**
 * AndroidClassLoader
 * 
 * Loads classes dynamically from dex files wrapped inside a zip.
 */
@SuppressWarnings("unchecked")
public class AndroidClassLoader extends ClassLoader //extends WebAppClassLoader
{
    private WebAppContext _context;
    private ClassLoader _parent;
    private ClassLoader _delegate;
    private String _path = "";


    public AndroidClassLoader(String path, ClassLoader parent, WebAppContext context) throws IOException
    {
        this(parent, context);
        _path = path;
    }

    public AndroidClassLoader(WebAppContext context)
    throws IOException
    {
        this (ClassLoader.getSystemClassLoader(), context);
    }
    
    public AndroidClassLoader(ClassLoader parent, WebAppContext context)
    throws IOException
    {
        //super(parent, context);
        _parent = parent;
        _context = context;
    }

    public WebAppContext getContext ()
    {
        return _context;
    }

    public void init ()
    throws IOException
    {
        if (_path==null || "".equals(_path.trim()))
            _delegate = new DexClassLoader("", ((WebAppContext)getContext()).getTempDirectory().getCanonicalPath(),null,_parent);
        else
            _delegate = new DexClassLoader(_path, ((WebAppContext)getContext()).getTempDirectory().getCanonicalPath(), null, _parent);
        
        if (Log.isDebugEnabled()) Log.debug("Android webapp classloader path= "+_path+" tmpdir="+ ((WebAppContext)getContext()).getTempDirectory()+" dexloader = "+_delegate+" parentloader="+_parent);
    }



    public void addClassPath(Resource resource)
    throws IOException
    {
        if (resource instanceof ResourceCollection)
        {
            for (Resource r : ((ResourceCollection)resource).getResources())
                addClassPath(r);
        }
        else
        {
            addClassPath(resource.getFile().getAbsolutePath());
        }
    }

   


    /** 
     * Accept a pre-made classpath. 
     * NOTE: the path elements must be separated by ":" chars, not ";"
     * @see org.eclipse.jetty.webapp.WebAppClassLoader#addClassPath(java.lang.String)
     */
    public void addClassPath(String classPath) throws IOException
    {
        if (classPath == null)
            return;
        
        if (!"".equals(_path) && !_path.endsWith(":"))
            _path += ":";
        
        _path += classPath; 
        Log.debug("Path = "+_path);
    }


    public void addJars(Resource lib)
    {
        if (lib.exists() && lib.isDirectory())
        {
            String[] files=lib.list();
            for (int f=0;files!=null && f<files.length;f++)
            {
                try 
                {
                    Resource fn=lib.addPath(files[f]);
                    String fnlc=fn.getName().toLowerCase();
                    
                    if (!fn.isDirectory() && isAndroidArchive(fnlc))
                    {
                        String jar=fn.getFile().getAbsolutePath();
                        addClassPath(jar);
                    }
                }
                catch (Exception ex)
                {
                    Log.warn(Log.EXCEPTION,ex);
                }
            }
        }
    }
    
    
    
    protected boolean isAndroidArchive (String filename)
    {
        int dot = filename.lastIndexOf('.');
        if (dot == -1)
            return false;
        
        String extension = filename.substring(dot);
        return ".zip".equals(extension) || ".apk".equals(extension);
    }
    
    public Enumeration<URL> getResources(String name) throws IOException
    {
        boolean system_class=_context.isSystemClass(name);
        boolean server_class=_context.isServerClass(name);
        
        List<URL> from_parent = toList(server_class?null:_parent.getResources(name));
        List<URL> from_webapp = toList((system_class&&!from_parent.isEmpty())?null:this.findResources(name));
            
        if (_context.isParentLoaderPriority())
        {
            from_parent.addAll(from_webapp);
            return Collections.enumeration(from_parent);
        }
        from_webapp.addAll(from_parent);
        return Collections.enumeration(from_webapp);
    }


    private List<URL> toList(Enumeration<URL> e)
    {
        List<URL> list = new ArrayList<URL>();
        while (e!=null && e.hasMoreElements())
            list.add(e.nextElement());
        return list;
    }
    

    public URL getResource(String name)
    {
        URL url= null;
        boolean tried_parent= false;
        boolean system_class=_context.isSystemClass(name);
        boolean server_class=_context.isServerClass(name);
        
        if (system_class && server_class)
            return null;
        
        if (_parent!=null &&(_context.isParentLoaderPriority() || system_class ) && !server_class)
        {
            tried_parent= true;
            
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url == null)
        {
            url= this.findResource(name);

            if (url == null && name.startsWith("/"))
            {
                if (Log.isDebugEnabled())
                    Log.debug("HACK leading / off " + name);
                url= this.findResource(name.substring(1));
            }
        }

        if (url == null && !tried_parent && !server_class )
        {
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url != null)
            if (Log.isDebugEnabled())
                Log.debug("getResource("+name+")=" + url);

        return url;
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> c= findLoadedClass(name);
        ClassNotFoundException ex= null;
        boolean tried_parent= false;
        
        boolean system_class=((WebAppContext)getContext()).isSystemClass(name);
        boolean server_class=((WebAppContext)getContext()).isServerClass(name);
        
        if (system_class && server_class)
        {
            return null;
        }
        
        if (c == null && _parent!=null && (((WebAppContext)getContext()).isParentLoaderPriority() || system_class) && !server_class)
        {
            tried_parent= true;
            try
            {
                c= _parent.loadClass(name);
                if (Log.isDebugEnabled())
                    Log.debug("loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null)
        {
            try
            {
                if (_delegate != null)
                {
                    if (Log.isDebugEnabled()) Log.debug("loading class "+name+" trying delegate loader" +_delegate);
                    c = _delegate.loadClass(name);
                    if (Log.isDebugEnabled()) Log.debug("delegate loaded " + c);
                }
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }
        
        
        if (c == null && _parent!=null && !tried_parent && !server_class )
            c= _parent.loadClass(name);

        if (c == null)
            throw ex;

        if (resolve)
            resolveClass(c);

        if (Log.isDebugEnabled())
            Log.debug("loaded " + c+ " from "+c.getClassLoader());
        
        return c;
    }


    @Override
    public String toString()
    {
    	return "(AndroidClassLoader, delegate=" + _delegate + ")";
    }
}
