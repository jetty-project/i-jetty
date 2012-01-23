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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.IO;

import android.util.Log;

public class Installer
{ 
    private static final String TAG = "Jetty.install";
    

                                        
    
    public static void install (File warFile, String contextPath, File webappsDir, String webappName, boolean createContextXml) 
    throws MalformedURLException, IOException
    {
        File webapp = new File (webappsDir, webappName);
        if (!webapp.exists())
            webapp.mkdirs();
        
        Resource war = Resource.newResource("jar:"+warFile.toURL()+"!/");
        ((JarResource)war).copyTo(webapp);
        if (createContextXml)
            installContextFile(webappName, contextPath);
    }
    
    
    public static void install (InputStream warStream, String contextPath, File webappsDir, String webappName, boolean createContextXml)
    {
        File webapp = new File (webappsDir, webappName);
        if (warStream != null)
        {
            try
            {
                JarInputStream jin = new JarInputStream(warStream);
                JarEntry entry;
                while((entry=jin.getNextJarEntry())!=null)
                {
                    String entryName = entry.getName();             
                    File file=new File(webapp,entryName);
                    if (entry.isDirectory())
                    {
                        // Make directory
                        if (!file.exists())
                            file.mkdirs();
                    }
                    else
                    {
                        // make directory (some jars don't list dirs)
                        File dir = new File(file.getParent());
                        if (!dir.exists())
                            dir.mkdirs();

                        // Make file
                        FileOutputStream fout = null;
                        try
                        {
                            fout = new FileOutputStream(file);
                            IO.copy(jin,fout);
                        }
                        finally
                        {
                            IO.close(fout);
                        }

                        // touch the file.
                        if (entry.getTime()>=0)
                            file.setLastModified(entry.getTime());
                    }
                }
                IO.close(jin);

                if (createContextXml)
                    installContextFile (webappName, contextPath);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Error inflating console.war", e);
            }
        }
        else
            Log.e(TAG, "No war");
    }
    
    
    public static void installContextFile (String webappName, String contextPath) 
    throws FileNotFoundException
    {
        Log.i(TAG, "Installing "+webappName+".xml");
        contextPath = contextPath == null ? webappName : contextPath;           
        contextPath = contextPath.equals("/") ? "root" : contextPath;
        contextPath = contextPath.startsWith("/") ? contextPath : "/"+contextPath;
        
        
        String configurationClassesXml =  "<Array type=\"java.lang.String\">";
        for (int i=0; i < IJettyService.__configurationClasses.length;i++)
        {
            configurationClassesXml +="<Item>"+IJettyService.__configurationClasses[i]+"</Item>";
        }
        configurationClassesXml += "</Array>";
        
        File tmpDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__TMP_DIR);       
        File tmpContextFile = new File (tmpDir, webappName+".xml");
       
        PrintWriter writer = new PrintWriter(tmpContextFile);
        writer.println("<?xml version=\"1.0\"  encoding=\"ISO-8859-1\"?>");
        writer.println("<!DOCTYPE Configure PUBLIC \"-//Jetty//Configure//EN\" \"http://www.eclipse.org/jetty/configure.dtd\">"
);
        writer.println("<Configure class=\"org.eclipse.jetty.webapp.WebAppContext\">");
        writer.println("<Set name=\"configurationClasses\">"+configurationClassesXml+"</Set>");   
        writer.println("<Set name=\"contextPath\">"+contextPath+"</Set>");
        writer.println("<Set name=\"war\"><SystemProperty name=\"jetty.home\" default=\".\"/>/webapps/"+webappName+"</Set>");
        writer.println("<Set name=\"defaultsDescriptor\"><SystemProperty name=\"jetty.home\" default=\".\"/>/etc/webdefault.xml</Set>");
       
        writer.println("</Configure>");
        writer.flush();
        writer.close();
        File contextDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__CONTEXTS_DIR); 
        File contextFile = new File (contextDir, webappName+".xml");
        if (!tmpContextFile.renameTo(contextFile))
            Log.e(TAG, "mv "+tmpContextFile.getAbsolutePath()+" "+contextFile.getAbsolutePath()+" failed");
    }

    
    public static void clean (File warFile)
    {
        String webappName = warFile.getName();
        if (webappName.endsWith(".war") || webappName.endsWith(".jar"))
            webappName = webappName.substring(0, webappName.length()-4);

        //delete any tmp context.xml file left over
        File tmpDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__TMP_DIR);  
        File contextFile = new File (tmpDir, webappName+".xml");
        contextFile.delete();
        Log.i("Jetty", "deleted "+IJetty.__JETTY_DIR+"/"+IJetty.__TMP_DIR+"/"+webappName+".xml");
        
        //delete the real context.xml file (will cause an undeploy if jetty is running)
        File contextDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__CONTEXTS_DIR); 
        contextFile = new File (contextDir, webappName+".xml");
        contextFile.delete();
        Log.i("Jetty", "deleted "+IJetty.__JETTY_DIR+"/"+IJetty.__CONTEXTS_DIR+"/"+ webappName+".xml");
        
        //delete the unpacked webapp
        File webappsDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__WEBAPP_DIR);
        File webapp = new File (webappsDir, webappName);
        
        if (webapp.exists())
            delete(webapp);
        Log.i(TAG, "deleted "+IJetty.__JETTY_DIR+"/"+IJetty.__WEBAPP_DIR+"/"+webappName);
        
        warFile.delete();
        Log.i(TAG, "deleted "+warFile.getAbsolutePath());
    }

    public static void delete (File webapp)
    {
        if (webapp.isDirectory())
        {
            File[] files = webapp.listFiles();
            for (File f:files)
            {
                delete(f);
            }
            webapp.delete();
        }
        else
            webapp.delete();
    }
   
}
