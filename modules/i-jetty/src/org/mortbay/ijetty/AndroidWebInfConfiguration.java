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
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;



public class AndroidWebInfConfiguration extends WebInfConfiguration
{

    public void configureClassLoader() throws Exception
    {
        if (getWebAppContext().isStarted())
        {
            if (Log.isDebugEnabled()) Log.debug(getWebAppContext()+": Cannot configure webapp after it is started");
            return;
        }

        Resource web_inf = _context.getWebInf();

        ClassLoader parentLoader = this.getClass().getClassLoader();

        //Make a loader to use in case there is no WEB-INF
        AndroidClassLoader loader = new AndroidClassLoader(null, parentLoader, _context); 

        //Make a loader containing all .zip files in WEB-INF/lib
        if (web_inf != null && web_inf.isDirectory())
        {
            Resource lib = web_inf.addPath("lib/");
            String paths = "";   
            if (lib.exists() || lib.isDirectory())
            {                 
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
                loader = new AndroidClassLoader (paths, parentLoader, _context);
            }
            else
                if (Log.isDebugEnabled()) Log.debug("No WEB-INF/lib for "+_context.getContextPath());
        }
        else
            if (Log.isDebugEnabled()) Log.debug("No WEB-INF for "+_context.getContextPath());

        if (_context.getClassLoader() != null)
            Log.warn ("Ignoring classloader "+_context.getClassLoader());

        _context.setClassLoader (loader);
    }
}
