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

package org.mortbay.ijetty;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ClassNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import android.util.Log;
import dalvik.system.PathClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * AndroidClassLoader
 * 
 * Loads classes dynamically from dex files wrapped inside a zip.
 * 
 * 
 */
public class AndroidClassLoader extends ClassLoader
{
    private ClassLoader _parent;
    private ClassLoader _delegate;
    private WebAppContext _context;


    public AndroidClassLoader(String path, ClassLoader parent, WebAppContext context)
    {
        // _parent = parent;
        _parent = this.getClass().getClassLoader();
         Log.i("Jetty", "System classloader="+ClassLoader.getSystemClassLoader()+" class loader="+this.getClass().getClassLoader());
         if (_parent == null)
             _parent = ClassLoader.getSystemClassLoader();
   
        if (path==null || "".equals(path.trim()))
            _delegate = new PathClassLoader("", _parent);
        else
            _delegate = new PathClassLoader(path, _parent);

        _context = context;
    }



    
    public synchronized URL getResource(String name)
    {
        URL url= null;
        boolean tried_parent= false;
        if (_context.isParentLoaderPriority() || isSystemPath(name))
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
                Log.d("Jetty", "HACK leading / off " + name);
                url= this.findResource(name.substring(1));
            }
        }

        if (url == null && !tried_parent)
        {
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url != null)
            Log.d("Jetty", "getResource("+name+")=" + url);

        return url;
    }


    public boolean isServerPath(String name)
    {
        name=name.replace('/','.');
        while(name.startsWith("."))
            name=name.substring(1);

        String[] server_classes = _context.getServerClasses();
        if (server_classes!=null)
        {
            for (int i=0;i<server_classes.length;i++)
            {
                boolean result=true;
                String c=server_classes[i];
                if (c.startsWith("-"))
                {
                    c=c.substring(1); // TODO cache
                    result=false;
                }
                
                if (c.endsWith("."))
                {
                    if (name.startsWith(c))
                        return result;
                }
                else if (name.equals(c))
                    return result;
            }
        }
        return false;
    }

    public boolean isSystemPath(String name)
    {
        name=name.replace('/','.');
        while(name.startsWith("."))
            name=name.substring(1);
        String[] system_classes = _context.getSystemClasses();
        if (system_classes!=null)
        {
            for (int i=0;i<system_classes.length;i++)
            {
                boolean result=true;
                String c=system_classes[i];
                
                if (c.startsWith("-"))
                {
                    c=c.substring(1); // TODO cache
                    result=false;
                }
                
                if (c.endsWith("."))
                {
                    if (name.startsWith(c))
                        return result;
                }
                else if (name.equals(c))
                    return result;
            }
        }
        
        return false;
        
    }

   public synchronized Class loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name, false);
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class c= findLoadedClass(name);
        ClassNotFoundException ex= null;
        boolean tried_parent= false;
        Log.i("Jetty", _context.getContextPath()+" parent priority = "+_context.isParentLoaderPriority());
        
        if (c == null && _parent!=null && (_context.isParentLoaderPriority() || isSystemPath(name)) )
        {
            tried_parent= true;
            try
            {
                Log.i("Jetty", "loading class "+name+" trying parent loader first" + _parent);
                c= _parent.loadClass(name);
                Log.i("Jetty", "parent loaded " + c);
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
                Log.i("Jetty", "loading class "+name+" trying delegate loader" +_delegate);
                c= _delegate.loadClass(name);
                Log.i("Jetty", "delegate loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null && _parent!=null && !tried_parent && !isServerPath(name) )
            c= _parent.loadClass(name);

        if (c == null)
            throw ex;

        if (resolve)
            resolveClass(c);

        Log.d("Jetty", "loaded " + c+ " from "+c.getClassLoader());
        
        return c;
    }


    @Override
    public String toString()
    {
    	return "(AndroidClassLoader, delegate=" + _delegate + ")";
    }
}
