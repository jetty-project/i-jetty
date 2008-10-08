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

import java.util.ArrayList;

import org.mortbay.jetty.deployer.WebAppDeployer;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import org.mortbay.util.URIUtil;

import dalvik.system.PathClassLoader;
import android.content.ContentResolver;
import android.util.Log;

/**
 * Web Application Deployer.
 * 
 * The class searches a directory for and deploys standard web application. At
 * startup, the directory specified by {@link #setWebAppDir(String)} is searched
 * for subdirectories (excluding hidden and CVS) or files ending with ".zip" or
 * "*.war". For each webapp discovered is passed to a new instance of
 * {@link WebAppContext} (or a subclass specified by {@link #getContexts()}.
 * {@link ContextHandlerCollection#getContextClass()}
 * 
 * This deployer does not do hot deployment or undeployment. Nor does it support
 * per webapplication configuration. For these features see
 * {@link ContextDeployer}.
 * 
 * @see {@link ContextDeployer}
 */
public class AndroidWebAppDeployer extends WebAppDeployer
{
    private ArrayList _deployed;
    private ContentResolver _resolver = null;

    public void setContentResolver(ContentResolver resolver)
    {
        _resolver = resolver;
    }
    
    public ContentResolver getContentResolver()
    {
        return _resolver;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @throws Exception
     */
    public void doStart() throws Exception
    {
        _deployed = new ArrayList();
        scan();

    }

    /* ------------------------------------------------------------ */
    /**
     * Scan for webapplications.
     * 
     * @throws Exception
     */
    public void scan() throws Exception
    {
        if (getContexts() == null)
            throw new IllegalArgumentException("No HandlerContainer");

        Resource r = Resource.newResource(getWebAppDir());
        Log.i ("Jetty", "Web application directory: " + getWebAppDir());
        if (!r.exists())
            throw new IllegalArgumentException("No such webapps resource " + r);

        if (!r.isDirectory())
            throw new IllegalArgumentException(
                    "Not directory webapps resource " + r);

        String[] files = r.list();

        files: for (int f = 0; files != null && f < files.length; f++)
        {
            String context = files[f];
            Log.d ("Jetty", "Have file: " + context);

            if (context.equalsIgnoreCase("CVS/")
                    || context.equalsIgnoreCase("CVS")
                    || context.startsWith(".")) continue;

            Log.d ("Jetty", "Adding resource to path.");
            Resource app = r.addPath(r.encode(context));

            if (context.toLowerCase().endsWith(".war")
                    || context.toLowerCase().endsWith(".jar"))
            {
                context = context.substring(0, context.length() - 4);
                Resource unpacked = r.addPath(context);
                Log.d ("Jetty", "Want to unpack!");
                if (unpacked != null && unpacked.exists()
                        && unpacked.isDirectory())
                {
                    Log.d ("Jetty", "already exists.");
                    continue;
                }
            }
            else if (!app.isDirectory())
            {
                Log.d ("Jetty", "Is directory, so skipping...");
                continue;
            }

            if (context.equalsIgnoreCase("root")
                    || context.equalsIgnoreCase("root/"))
                context = URIUtil.SLASH;
            else
                context = "/" + context;
            if (context.endsWith("/") && context.length() > 0)
                context = context.substring(0, context.length() - 1);

            // Check the context path has not already been added or the webapp
            // itself is not already deployed
            if (!getAllowDuplicates())
            {
                Handler[] installed = getContexts().getChildHandlersByClass(
                        ContextHandler.class);
                for (int i = 0; i < installed.length; i++)
                {
                    ContextHandler c = (ContextHandler) installed[i];

                    if (context.equals(c.getContextPath())){
                        Log.d ("Jetty", "Context were equal; duplicate!");
                        continue files;
                    }

                    String path;
                    if (c instanceof WebAppContext)
                        path = ((WebAppContext) c).getWar();
                    else
                        path = (c.getBaseResource() == null ? "" : c
                                .getBaseResource().getFile().getAbsolutePath());

                    if (path.equals(app.getFile().getAbsolutePath()))
                    {
                        Log.d ("Jetty", "Paths were equal; duplicate!");
                        continue files;
                    }

                }
            }

            Log.d("Jetty", "AndroidWebAppDeployer, context="+context);
            // create a webapp
            WebAppContext wah = null;
            HandlerContainer contexts = getContexts();
            if (contexts instanceof ContextHandlerCollection
                    && 
               WebAppContext.class.isAssignableFrom(((ContextHandlerCollection) contexts).getContextClass()))
            {
                try
                {
                    wah = (WebAppContext) ((ContextHandlerCollection) contexts).getContextClass().newInstance();
                }
                catch (Exception e)
                {
                    throw new Error(e);
                }
            }
            else
            {
                wah = new WebAppContext();
            }

            // configure it
            wah.setContextPath(context);

            if (getConfigurationClasses() != null)
            {
                wah.setConfigurationClasses(getConfigurationClasses());
                Log.d ("Jetty", "AndroidWebAppDeployer: Set configuration classes");
            }


            if (getDefaultsDescriptor() != null) 
            {
                Log.d ("Jetty", "Setting defaults descriptor to: " + getDefaultsDescriptor());
                wah.setDefaultsDescriptor(getDefaultsDescriptor());
            }
            wah.setExtractWAR(isExtract()); 
            wah.setWar(app.toString());
            wah.setParentLoaderPriority(isParentLoaderPriority());
            if (_resolver != null)
                wah.setAttribute("contentResolver", _resolver);
            // add it
            Log.d ("Jetty", "AndroidWebAppDeployer: prepared " + app.toString());
            contexts.addHandler(wah);
            _deployed.add(wah);
        }
    }

    public void doStop() throws Exception
    {
        for (int i = _deployed.size(); i-- > 0;)
        {
            ContextHandler wac = (ContextHandler) _deployed.get(i);
            wac.stop();// TODO Multi exception
        }
    }
}
