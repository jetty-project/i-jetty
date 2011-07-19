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

package org.mortbay.ijetty.deployer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.deploy.ContextDeployer;
import org.eclipse.jetty.deploy.WebAppDeployer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.AttributesMap;
import org.eclipse.jetty.util.URIUtil;

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
public class AndroidWebAppDeployer extends WebAppDeployer {
    private List<? super ServletContextHandler>     _deployed;
    private AttributesMap _attributes = new AttributesMap();

    /* ------------------------------------------------------------ */
    /**
     * @throws Exception
     */
    @Override
    public void doStart() throws Exception {
        _deployed = new ArrayList<ServletContextHandler>();
        scan();

    }

    @Override
    public void doStop() throws Exception {
        for (int i = _deployed.size(); i-- > 0;) {
            ContextHandler wac = (ContextHandler) _deployed.get(i);
            wac.stop();// TODO Multi exception
        }
    }

    public Object getAttribute(String name) {
        return _attributes.getAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /**
     * Scan for webapplications.
     * 
     * @throws Exception
     */
    @Override
    public void scan() throws Exception {
        if (getContexts() == null) {
            throw new IllegalArgumentException("No HandlerContainer");
        }

        Resource r = Resource.newResource(getWebAppDir());
        if (!r.exists()) {
            throw new IllegalArgumentException("No such webapps resource " + r);
        }

        if (!r.isDirectory()) {
            throw new IllegalArgumentException("Not directory webapps resource " + r);
        }

        String[] files = r.list();

        files: for (int f = 0; (files != null) && (f < files.length); f++) {
            String context = files[f];

            if (context.equalsIgnoreCase("CVS/") || context.equalsIgnoreCase("CVS") || context.startsWith(".")) {
                continue;
            }

            Resource app = r.addPath(r.encode(context));

            if (context.toLowerCase().endsWith(".war") || context.toLowerCase().endsWith(".jar")) {
                context = context.substring(0, context.length() - 4);
                Resource unpacked = r.addPath(context);

                if ((unpacked != null) && unpacked.exists() && unpacked.isDirectory()) {
                    Log.debug(context + " already exists.");
                    continue;
                }
            } else if (!app.isDirectory()) {
                Log.debug(app + " Not directory");
                continue;
            }

            if (context.equalsIgnoreCase("root") || context.equalsIgnoreCase("root/")) {
                context = URIUtil.SLASH;
            } else {
                context = "/" + context;
            }
            if (context.endsWith("/") && (context.length() > 0)) {
                context = context.substring(0, context.length() - 1);
            }

            // Check the context path has not already been added or the webapp
            // itself is not already deployed
            if (!getAllowDuplicates()) {
                Handler[] installed = getContexts().getChildHandlersByClass(ContextHandler.class);
                for (int i = 0; i < installed.length; i++) {
                    ContextHandler c = (ContextHandler) installed[i];

                    if (context.equals(c.getContextPath())) {
                        if (Log.isDebugEnabled()) {
                            Log.debug(context + " Context were equal; duplicate!");
                        }
                        continue files;
                    }

                    String path;
                    if (c instanceof WebAppContext) {
                        path = Resource.newResource(((WebAppContext) c).getWar()).getFile().getAbsolutePath();
                    } else {
                        path = c.getBaseResource().getFile().getAbsolutePath();
                    }

                    if ((path != null) && path.equals(app.getFile().getAbsolutePath())) {
                        if (Log.isDebugEnabled()) {
                            Log.debug(path + " Paths were equal; duplicate!");
                        }
                        continue files;
                    }

                }
            }

            // create a webapp
            WebAppContext wah = null;
            HandlerCollection contexts = getContexts();
            if ((contexts instanceof ContextHandlerCollection)
                    && WebAppContext.class.isAssignableFrom(((ContextHandlerCollection) contexts).getContextClass())) {
                try {
                    wah = (WebAppContext) ((ContextHandlerCollection) contexts).getContextClass().newInstance();
                } catch (Exception e) {
                    throw new Error(e);
                }
            } else {
                wah = new WebAppContext();
            }

            // configure it
            wah.setContextPath(context);

            if (getConfigurationClasses() != null) {
                wah.setConfigurationClasses(getConfigurationClasses());
            }

            if (getDefaultsDescriptor() != null) {
                wah.setDefaultsDescriptor(getDefaultsDescriptor());
            }
            wah.setExtractWAR(isExtract());
            wah.setWar(app.toString());
            wah.setParentLoaderPriority(isParentLoaderPriority());

            Enumeration<?> names = _attributes.getAttributeNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                wah.setAttribute(name, _attributes.getAttribute(name));
            }

            // add it
            Log.debug("AndroidWebAppDeployer: prepared " + app.toString());
            contexts.addHandler(wah);
            _deployed.add(wah);
            //jetty-7.3.0 onwards need to start explicitly due to different startup time ordering
            wah.start();
        }
    }

    public void setAttribute(String name, Object value) {
        _attributes.setAttribute(name, value);
    }
}
