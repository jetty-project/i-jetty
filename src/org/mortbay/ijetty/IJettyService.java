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


import java.io.InputStream;
import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.mortbay.ijetty.servlet.CallLogServlet;
import org.mortbay.ijetty.servlet.ContactsServlet;
import org.mortbay.ijetty.servlet.CssServlet;
import org.mortbay.ijetty.servlet.IPServlet;
import org.mortbay.ijetty.servlet.IndexServlet;
import org.mortbay.ijetty.servlet.InfoFilter;
import org.mortbay.ijetty.servlet.SettingsServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;

public class IJettyService extends Service
{

    private NotificationManager mNM;
    private Server server;
    private static Resources __resources;
    private SharedPreferences preferences;


    @Override
    protected void onCreate() 
    {
        try
        {
            __resources = getResources();
            startJetty();
            preferences = getSharedPreferences("jetty", MODE_WORLD_READABLE);
            Editor editor = preferences.edit();
            editor.putBoolean("isRunning", true);
            editor.commit();
            mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

            // This is who should be launched if the user selects our persistent
            // notification.
            Intent intent = new Intent(this, IJetty.class);

            Toast.makeText(IJettyService.this, R.string.jetty_started, Toast.LENGTH_SHORT).show();
            mNM.notify(R.string.jetty_started,
                    new Notification(
                            this,
                            R.drawable.jicon,
                            getText(R.string.manage_jetty),
                            System.currentTimeMillis(),
                            getText(R.string.manage_jetty),
                            getText(R.string.manage_jetty),
                            intent,
                            R.drawable.jicon,
                            getText(R.string.manage_jetty),
                            intent));
            Log.i("Jetty", "Jetty started");  
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error starting jetty", e);
            Toast.makeText(this, getText(R.string.jetty_not_started), Toast.LENGTH_SHORT).show();
        }  
    }

    @Override
    protected void onDestroy() {

        try
        {
            if (server != null)
            {
                stopJetty();
                // Cancel the persistent notification.
                mNM.cancel(R.string.jetty_started);
                // Tell the user we stopped.
                Toast.makeText(this, getText(R.string.jetty_stopped), Toast.LENGTH_SHORT).show();
                Editor editor = preferences.edit();
                editor.putBoolean("isRunning", false);
                editor.commit();
                Log.i("Jetty", "etty stopped");
                __resources=null;
            }
            else
                Log.i("Jetty", "Jetty not running");

        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error stopping jetty", e);
            Toast.makeText(this, getText(R.string.jetty_not_stopped), Toast.LENGTH_SHORT).show();
        }  
    }


	
	
	/**
	 * Hack to get around bug in ResourceBundles
	 * @param id
	 * @return
	 */
	public static InputStream getStreamToRawResource (int id)
	{
	    if (__resources != null)
	        return __resources.openRawResource(id);  
	    else
	        return null;
	}

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    private void startJetty()
    throws Exception
    {
        Log.w ("Jetty", "HOLY SMOKES BATMAN! THIS IS NEW!");
        // TODO - get ports and types of connector from SharedPrefs?
        server = new Server();
        Connector connector=new SelectChannelConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});
        
        // Bridge Jetty logging to Android logging
        System.setProperty("org.mortbay.log.class","org.mortbay.log.AndroidLog");
        org.mortbay.log.Log.setLog(new AndroidLog());
        
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);
        
        // Load any webapps we find on the card.
        if (new File("/sdcard/jetty/").exists()) 
        {
            AndroidWebAppDeployer deployer = new AndroidWebAppDeployer();
            deployer.setWebAppDir("/sdcard/jetty/webapps");
            deployer.setDefaultsDescriptor("/sdcard/jetty/etc/webdefault.xml");
            deployer.setContexts (contexts);
            server.addLifeCycle(deployer);
            
            Log.i ("Jetty", "Added deployer to server lifecycle.");
        } 
        else
        {
            Log.w("Jetty", "Not loading any webapps - none on SD card.");
        }
        
        // Deploy some servlets to serve on--phone information
        Context context = new Context(contexts, "/", Context.SESSIONS);
        
        ContactsServlet contactsServlet = new ContactsServlet();
        contactsServlet.setContentResolver(getContentResolver());
        context.addServlet(new ServletHolder(contactsServlet), "/app/contacts/*");
        
        CallLogServlet callLogServlet = new CallLogServlet();
        callLogServlet.setContentResolver(getContentResolver());
        context.addServlet(new ServletHolder(callLogServlet), "/app/calls/*");
        
        SettingsServlet settingsServlet = new SettingsServlet();
        settingsServlet.setContentResolver(getContentResolver());
        context.addServlet(new ServletHolder(settingsServlet), "/app/settings/*");
        
        IPServlet ipServlet = new IPServlet();
        context.addServlet(new ServletHolder(ipServlet), "/app/network/*");
        
        IndexServlet indexServlet = new IndexServlet();
        context.addServlet(new ServletHolder(indexServlet), "/app");
        
        CssServlet cssServlet = new CssServlet();
        context.addServlet(new ServletHolder(cssServlet), "/app/css");
        context.addServlet(new ServletHolder(new org.mortbay.ijetty.servlet.DefaultServlet()) ,"/");
        context.addFilter(new FilterHolder(new InfoFilter()), "/", Handler.REQUEST);
        
        server.start();
    }
    
    private void stopJetty()
    throws Exception
    {
        Log.i("Jetty", "Jetty stopping");
        server.stop();
        server.join();
        server=null;       
    }
}
