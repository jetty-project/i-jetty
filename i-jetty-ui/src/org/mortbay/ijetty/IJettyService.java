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
import java.io.IOException;
import java.io.InputStream;

import org.mortbay.ijetty.util.IJettyToast;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpGenerator;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSocketConnector;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * IJettyService
 *
 * Android Service which runs the Jetty server, maintaining it in the active Notifications so that
 * the user can return to the IJetty Activity to control it at any time.
 */
public class IJettyService extends Service
{
    private static Resources __resources;
    private static final String CONTENT_RESOLVER_ATTRIBUTE = "org.mortbay.ijetty.contentResolver";
    private static final String ANDROID_CONTEXT_ATTRIBUTE = "org.mortbay.ijetty.context"; 
    
    public static final int __START_PROGRESS_DIALOG = 0;
    public static final int __STARTED = 0;
    public static final int __NOT_STARTED = 1;
    public static final int __STOPPED = 2;
    public static final int __NOT_STOPPED = 3;
    public static final int __STARTING = 4;
    public static final int __STOPPING = 5;
    
    public static final String[] __configurationClasses = 
        new String[]
        {
            "org.mortbay.ijetty.AndroidWebInfConfiguration",
            "org.mortbay.jetty.webapp.WebXmlConfiguration",
            "org.mortbay.jetty.webapp.JettyWebXmlConfiguration",
            "org.mortbay.jetty.webapp.TagLibConfiguration" 
        };
 
    private NotificationManager mNM;
    private Server server;
    private ContextHandlerCollection contexts;
    private boolean _useNIO;
    private boolean _useSSL;
    private int _port;
    private int _sslPort;
    private String _consolePassword;
    private String _keymgrPassword;
    private String _keystorePassword;
    private String _truststorePassword;
    private String _keystoreFile;
    private String _truststoreFile;
    private SharedPreferences preferences;
    private PackageInfo pi;
    private android.os.Handler _handler;

    PowerManager.WakeLock wakeLock;
    

    
    public class JettyStarterThread extends Thread
    {
        android.os.Handler _handler;
        
        public JettyStarterThread(android.os.Handler handler)
        {
            _handler = handler;
        }
        public void run ()
        {
            try
            {
                sendMessage(__STARTING);
                startJetty();
                sendMessage(__STARTED);
              
                Log.i("Jetty", "Jetty started");
            }
            catch (Exception e)
            {
                sendMessage(__NOT_STARTED);
                Log.e("Jetty", "Error starting jetty", e);
                
            }
        }
        
        public void sendMessage(int state)
        {
            Message msg = _handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("state", state);
            msg.setData(b);
            _handler.sendMessage(msg);
        }
    }
    
    
    public class JettyStopperThread extends Thread
    { 
        android.os.Handler _handler;
        
        public JettyStopperThread(android.os.Handler handler)
        {
            _handler = handler;
        }
        
        public void run ()
        {
            try
            {
                sendMessage(__STOPPING);
                stopJetty();
                Log.i("Jetty", "Jetty stopped");
                sendMessage(__STOPPED);
               
            }
            catch (Exception e)
            {
                
                sendMessage(__NOT_STOPPED);
                Log.e("Jetty", "Error stopping jetty", e);
            }
        }
        
        public void sendMessage(int state)
        {
            Message msg = _handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("state", state);
            msg.setData(b);
            _handler.sendMessage(msg);
        }
    }
    
    
    public IJettyService()
    {
        super();
        _handler = new android.os.Handler ()
        {
            public void handleMessage(Message msg) {
                switch (msg.getData().getInt("state"))
                {
                    case __STARTED:
                    {
                        IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_started);
                        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        // The PendingIntent to launch IJetty activity if the user selects this notification
                        PendingIntent contentIntent = PendingIntent.getActivity(IJettyService.this, 0,
                                new Intent(IJettyService.this, IJetty.class), 0);

                        CharSequence text = getText(R.string.manage_jetty);

                        Notification notification = new Notification(R.drawable.ijetty_stat, 
                                text, 
                                System.currentTimeMillis());

                        notification.setLatestEventInfo(IJettyService.this, getText(R.string.app_name),
                                text, contentIntent);

                        mNM.notify(R.string.jetty_started, notification);
                        break;
                    }
                    case __NOT_STARTED:
                    {
                        IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_not_started);
                        break;
                    }
                    case __STOPPED:
                    {
                        // Cancel the persistent notification.
                        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mNM.cancel(R.string.jetty_started);
                        // Tell the user we stopped.
                        IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_stopped);
                       
                        break;
                    }
                    
                    case __NOT_STOPPED:
                    {
                        IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_not_stopped);
                        break;
                    }
                    case __STARTING:
                    {
                        IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_starting);
                        break;
                    }                    
                    case __STOPPING:
                    {
                        IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_stopping);
                        break;
                    }
                }
               
            }
 
        };
    }
    /** 
     * Android Service create
     * @see android.app.Service#onCreate()
     */
    public void onCreate()
    {
        __resources = getResources();

        try
        {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0); 
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Unable to determine running jetty version");
        }
    }


    /** 
     * Android Service Start
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    public void onStart(Intent intent, int startId)
    {
        if (server != null)
        {
            IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_already_started);
            return;
        }

        try
        {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);

            String portDefault = getText(R.string.pref_port_value).toString();
            String sslPortDefault = getText(R.string.pref_ssl_port_value).toString();
            String pwdDefault = getText(R.string.pref_console_pwd_value).toString();
            
            String nioEnabledDefault = getText(R.string.pref_nio_value).toString();
            String sslEnabledDefault = getText(R.string.pref_ssl_value).toString();

            String portKey = getText(R.string.pref_port_key).toString();
            String sslPortKey = getText(R.string.pref_ssl_port_key).toString();
            String pwdKey = getText(R.string.pref_console_pwd_key).toString();
            String nioKey = getText(R.string.pref_nio_key).toString();
            String sslKey = getText(R.string.pref_ssl_key).toString();
            
            _useSSL = preferences.getBoolean(sslKey, Boolean.valueOf(sslEnabledDefault));
            _useNIO = preferences.getBoolean(nioKey, Boolean.valueOf(nioEnabledDefault));
            _port = Integer.parseInt(preferences.getString(portKey, portDefault));
            if (_useSSL)
            {
              _sslPort = Integer.parseInt(preferences.getString(sslPortKey, sslPortDefault));
              String defaultValue = getText(R.string.pref_keystore_pwd_value).toString();
              String key = getText(R.string.pref_keystore_pwd_key).toString();
              _keystorePassword = preferences.getString(key, defaultValue);
              
              defaultValue = getText(R.string.pref_keymgr_pwd_value).toString();
              key = getText(R.string.pref_keymgr_pwd_key).toString();
              _keymgrPassword = preferences.getString(key, defaultValue);
              
              defaultValue = getText(R.string.pref_truststore_pwd_value).toString();
              key = getText(R.string.pref_truststore_pwd_key).toString();
              _truststorePassword = preferences.getString(key, defaultValue);
              
              defaultValue = getText(R.string.pref_keystore_file).toString();
              key = getText(R.string.pref_keystore_file_key).toString();
              _keystoreFile = preferences.getString(key, defaultValue);
              
              defaultValue = getText(R.string.pref_truststore_file).toString();
              key = getText(R.string.pref_truststore_file_key).toString();
              _truststoreFile = preferences.getString(key, defaultValue);
            }

            _consolePassword = preferences.getString(pwdKey, pwdDefault);

            Log.d("Jetty", "pref port = "+_port);
            Log.d("Jetty", "pref use nio = "+_useNIO);
            Log.d("Jetty", "pref use ssl = "+_useSSL);
            Log.d("Jetty", "pref ssl port = "+_sslPort);
           
            //Get a wake lock to stop the cpu going to sleep
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "IJetty");
            wakeLock.acquire();

            new JettyStarterThread(_handler).start();

 
            super.onStart(intent, startId);
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error starting jetty", e);
            IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_not_started);
        }
    }


    /** 
     * Android Service destroy
     * @see android.app.Service#onDestroy()
     */
    public void onDestroy()
    {
        try
        {
            if (wakeLock != null)
            {
                wakeLock.release();
                wakeLock = null;
            }
            
            if (server != null)
            {
                new JettyStopperThread(_handler).start();
                
            }
            else
            {
                Log.i("Jetty", "Jetty not running");
                IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_not_running);
            }
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error stopping jetty", e);
            IJettyToast.showServiceToast(IJettyService.this,R.string.jetty_not_stopped);
        }
    }
    
    
   
    

    public void onLowMemory()
    {
        Log.i("Jetty", "Low on memory");
        super.onLowMemory();
    }


    /**
     * Hack to get around bug in ResourceBundles
     * 
     * @param id
     * @return
     */
    public static InputStream getStreamToRawResource(int id)
    {
        if (__resources != null)
            return __resources.openRawResource(id);
        else
            return null;
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }

    
    /**
     * Get a reference to the Jetty Server instance
     * @return
     */
    public Server getServer()
    {
        return server;
    }
    
    
    protected Server newServer()
    {
        return new Server();
    }
    
    protected ContextHandlerCollection newContexts()
    {
        return new ContextHandlerCollection();
    }
  
    
    protected void configureConnectors()
    {
        if (server != null)
        {
            if (_useNIO)
            {
                SelectChannelConnector nioConnector = new SelectChannelConnector();
                nioConnector.setUseDirectBuffers(false);
                nioConnector.setPort(_port);
                server.addConnector(nioConnector);
            }
            else
            {
                SocketConnector bioConnector = new SocketConnector();
                bioConnector.setPort(_port);
                bioConnector.setMaxIdleTime(3000);
                server.addConnector(bioConnector);
            }

            if (_useSSL)
            {
              SslSocketConnector sslConnector = new SslSocketConnector();
              sslConnector.setPort(_sslPort);
              //sslConnector.setKeystore("/sdcard/jetty/etc/keystore");
              sslConnector.setKeystore(_keystoreFile);
              sslConnector.setKeystoreType("bks");
              //sslConnector.setTruststore("/sdcard/jetty/etc/keystore");
              sslConnector.setTruststore(_truststoreFile);
              //sslConnector.setPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
              sslConnector.setPassword(_keystorePassword);
              //sslConnector.setKeyPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
              sslConnector.setKeyPassword(_keymgrPassword);
              //sslConnector.setTrustPassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
              sslConnector.setTrustPassword(_truststorePassword);
              sslConnector.setTruststoreType("bks");
              server.addConnector(sslConnector);
            }
        }
    }
    
    protected void configureHandlers()
    {
        if (server != null)
        {
            HandlerCollection handlers = new HandlerCollection();
            contexts = new ContextHandlerCollection();
            handlers.setHandlers(new Handler[] {contexts, new DefaultHandler()});
            server.setHandler(handlers);
        }
    }
    
    protected void configureDeployers () throws Exception
    {
        AndroidWebAppDeployer staticDeployer =  new AndroidWebAppDeployer();
        AndroidContextDeployer contextDeployer = new AndroidContextDeployer();
     
        File jettyDir = IJetty.__JETTY_DIR;
        
        // Load any webapps we find on the card.
        if (jettyDir.exists())
        {
            // Deploy any static webapps we have.
            if (new File(jettyDir, IJetty.__WEBAPP_DIR).exists())
            {
                staticDeployer.setWebAppDir(IJetty.__JETTY_DIR+"/"+IJetty.__WEBAPP_DIR);
                staticDeployer.setDefaultsDescriptor(IJetty.__JETTY_DIR+"/"+IJetty.__ETC_DIR+"/webdefault.xml");
                staticDeployer.setContexts(contexts);
                staticDeployer.setAttribute(CONTENT_RESOLVER_ATTRIBUTE, getContentResolver());
                staticDeployer.setAttribute(ANDROID_CONTEXT_ATTRIBUTE, (Context) IJettyService.this);
                staticDeployer.setConfigurationClasses(__configurationClasses);
                staticDeployer.setAllowDuplicates(false);
            }          
           
            // Use a ContextDeploy so we can hot-deploy webapps and config at startup.
            if (new File(jettyDir, IJetty.__CONTEXTS_DIR).exists())
            {
                contextDeployer.setScanInterval(10); // Don't eat the battery
                contextDeployer.setConfigurationDir(IJetty.__JETTY_DIR+"/"+IJetty.__CONTEXTS_DIR);                
                contextDeployer.setAttribute(CONTENT_RESOLVER_ATTRIBUTE, getContentResolver());
                contextDeployer.setAttribute(ANDROID_CONTEXT_ATTRIBUTE, (Context) IJettyService.this);             
                contextDeployer.setContexts(contexts);
            }
        }
        else
        {
            Log.w("Jetty", "Not loading any webapps - none on SD card.");
        }

        if (server != null)
        {
            server.addLifeCycle(contextDeployer);
            server.addLifeCycle(staticDeployer); 
        }
    }
    
    public void configureRealm () throws IOException
    {
        File realmProps = new File(IJetty.__JETTY_DIR+"/"+IJetty.__ETC_DIR+"/realm.properties");
        if (realmProps.exists())
        {
            HashUserRealm realm = new HashUserRealm("Console", IJetty.__JETTY_DIR+"/"+IJetty.__ETC_DIR+"/realm.properties");
            realm.setRefreshInterval(0);
            if (_consolePassword != null)
                realm.put("admin", _consolePassword); //set the admin password for console webapp
            server.addUserRealm(realm);
        }
    }
    
    
    protected void startJetty() throws Exception
    {
        // Bridge Jetty logging to Android logging
        // TODO: Make configurable 
        AndroidLog.__isDebugEnabled = false;
        System.setProperty("org.mortbay.log.class","org.mortbay.log.AndroidLog");
        org.mortbay.log.Log.setLog(new AndroidLog());

        //Set jetty.home
        System.setProperty ("jetty.home", IJetty.__JETTY_DIR.getAbsolutePath());
        
        server = newServer();
        
        configureConnectors();
        configureHandlers();
        configureDeployers();
        configureRealm ();

        server.start();
        
        //TODO
        // Less than ideal solution to the problem that dalvik doesn't know about manifests of jars.
        // A as the version field is private to Server, its difficult
        //if not impossible to set it any other way. Note this means that ContextHandler.SContext.getServerInfo()
        //will still return 0.0.
        HttpGenerator.setServerVersion("i-jetty "+pi.versionName);
    }

    protected void stopJetty() throws Exception
    {
        try
        {
            Log.i("Jetty", "Jetty stopping");
            server.stop();
            Log.i("Jetty", "Jetty server stopped");
            server = null;
            __resources = null;
        }
        finally
        {
            Log.i("Jetty","Finally stopped");
        }
    }
}
