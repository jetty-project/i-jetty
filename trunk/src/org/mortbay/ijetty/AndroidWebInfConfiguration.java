package org.mortbay.ijetty;

import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebInfConfiguration;
import org.mortbay.resource.Resource;

import android.util.Log;

public class AndroidWebInfConfiguration extends WebInfConfiguration
{
    @Override
    public void configureClassLoader ()
    throws Exception
    {
        if (getWebAppContext().isStarted()) {
            //if (Log.isDebugEnabled()){Log.debug("Cannot configure webapp after it is started");}
            return;
        }
        
        Resource web_inf=_context.getWebInf();

        Log.d ("Jetty", "web_inf context: " + web_inf.toString ());
        
        // Add WEB-INF classes and lib classpaths
        if (web_inf != null && web_inf.isDirectory() && _context.getClassLoader() instanceof ClassLoader)
        {
            // Look for classes directory
            // FIXME: I doubt this would actually work; test this?
            // ALSO: Remember to change instanceof check to WebAppClassLoader! :)
            /*Resource classes= web_inf.addPath("classes/");
            if (classes.exists())
                ((WebAppClassLoader)_context.getClassLoader()).addClassPath(classes.toString());*/

            // Look for jars
            Resource lib= web_inf.addPath("lib/");
            Log.d ("Jetty", "Library resource: " + lib.toString ());
            if (lib.exists() || lib.isDirectory()) {
                Log.d ("Jetty", "Yes, lib/ path exists. Trying to list...");
                AndroidClassLoader loader = ((AndroidClassLoader)_context.getClassLoader());
                for (String dex : lib.list()) {
                    String fullpath = web_inf.addPath("lib/").addPath(dex).getFile().getAbsolutePath();
                    if (loader.addDexFile (fullpath)) {
                        Log.d ("Jetty", "Added DEX file to class loader: " + fullpath);
                    } else {
                        Log.w ("Jetty", "Failed to add DEX file from path: " + fullpath);
                    }
                    
                    // This is retarded, but we need to set it back after we're done.
                    _context.setClassLoader (loader);
                }
            }
        }
    }
}
