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

import org.mortbay.jetty.webapp.WebInfConfiguration;
import org.mortbay.resource.Resource;

import dalvik.system.PathClassLoader;
import android.util.Log;

public class AndroidWebInfConfiguration extends WebInfConfiguration
{

    public void configureClassLoader() throws Exception
    {
        if (getWebAppContext().isStarted())
        {
            Log.d("Jetty", "Cannot configure webapp after it is started");
            return;
        }
        
        Resource web_inf = _context.getWebInf();
      
       
        ClassLoader parentLoader = this.getClass().getClassLoader();

         AndroidClassLoader loader = new AndroidClassLoader(null, parentLoader, _context); 
                              /* new AndroidClassLoader(parentLoader, _context); */
        // Add WEB-INF lib classpath 
        if (web_inf != null && web_inf.isDirectory())
        {
            Resource lib = web_inf.addPath("lib/");
            String paths = "";   
            if (lib.exists() || lib.isDirectory())
            {                 
/*
                for (String dex : lib.list())
                {
                    String fullpath = web_inf.addPath("lib/").addPath(dex)
                            .getFile().getAbsolutePath();
                    if (!loader.addDexFile(fullpath))
                    {
                        Log.w("Jetty", "Failed to add DEX file from path: "+ fullpath);
                    }
                }
*/
                for (String dex : lib.list())
                {
                    if (dex.endsWith("zip") || dex.endsWith("apk"))
                    {
                      String fullpath = web_inf.addPath("lib/").addPath(dex).getFile().getAbsolutePath();
                      if (!"".equals(paths))
                          paths +=":";

                      paths += fullpath;
                    }
                }
                Log.d("Jetty", "webapp classloader paths = "+paths);
                loader = new AndroidClassLoader (paths, parentLoader, _context);
            }
            else
                Log.d("Jetty", "No WEB-INF/lib for "+_context.getContextPath());
        }
        else
            Log.d("Jetty", "No WEB-INF for "+_context.getContextPath());

        if (_context.getClassLoader() != null)
            Log.w ("Jetty", "Ignoring classloader "+_context.getClassLoader());

        Log.d("Jetty", "Android classloader "+loader);
        _context.setClassLoader (loader);
    }
}
