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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import org.mortbay.ijetty.util.AndroidInfo;
import org.mortbay.ijetty.util.IJettyToast;
import org.mortbay.jetty.Server;
import org.mortbay.util.IO;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * IJetty
 * 
 * Main Jetty activity. Can start other activities: + configure + download
 * 
 * Can start/stop services: + IJettyService
 */
public class IJetty extends Activity
{
    class ConsoleScrollTask implements Runnable
    {
        public void run()
        {
            consoleScroller.fullScroll(View.FOCUS_DOWN);
        }
    }
    private static final String TAG = "Jetty";
    public static final String __PORT = "org.mortbay.ijetty.port";
    public static final String __NIO = "org.mortbay.ijetty.nio";
    public static final String __SSL = "org.mortbay.ijetty.ssl";

    public static final String __CONSOLE_PWD = "org.mortbay.ijetty.console";
    public static final String __PORT_DEFAULT = "8080";
    public static final boolean __NIO_DEFAULT = true;
    public static final boolean __SSL_DEFAULT = false;

    public static final String __CONSOLE_PWD_DEFAULT = "admin";
    public static final File __JETTY_DIR;
    public static final String __WEBAPP_DIR = "webapps";
    public static final String __ETC_DIR = "etc";
    public static final String __CONTEXTS_DIR = "contexts";

    public static final String __TMP_DIR = "tmp";
    public static final String __WORK_DIR = "work";
    public static final int __SETUP_PROGRESS_DIALOG = 0;
    public static final int __SETUP_DONE = 2;
    public static final int __SETUP_RUNNING = 1;
    public static final int __SETUP_NOTDONE = 0;
    PackageInfo pi = null;
    private TextView console;
    private ScrollView consoleScroller;
    private StringBuilder out = new StringBuilder();
    private int setupState = -1;
    private Runnable scrollTask;
    private ProgressDialog progressDialog;
    private Thread progressThread;
    private Handler handler;

    /**
     * ProgressThread
     *
     * Handles finishing install tasks for Jetty.
     */
    class ProgressThread extends Thread
    {
        private Handler _handler;
    
        public ProgressThread(Handler h) {
            _handler = h;
        }

        public void sendProgressUpdate (int prog)
        { 
            Message msg = _handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("prog", prog);
            msg.setData(b);
            _handler.sendMessage(msg);
        }
        
        public void run ()
        {
            boolean updateNeeded = isUpdateNeeded();
            
            //create the jetty dir structure
            File jettyDir = __JETTY_DIR;
            if (!jettyDir.exists())
            {
                boolean made = jettyDir.mkdirs();
                Log.i(TAG,"Made " + __JETTY_DIR + ": " + made);
            }
            else
            {
                Log.i(TAG,__JETTY_DIR + " exists");

                // Always update if ${jetty.home}/.update exists (DEBUG)
                File alwaysUpdate = new File(jettyDir,".update");
                if (alwaysUpdate.exists())
                {
                    Log.i(TAG,"Always Update tag found " + alwaysUpdate);
                    updateNeeded = true;
                }
            }
            sendProgressUpdate(10);

            
            //set the directory into which all webapps should be unpacked
            //on deployment. Note this is a "work" directory, so the contents
            //will not be deleted each time on undeploy.
            File workDir = new File(jettyDir, __WORK_DIR);
            if (!workDir.exists())
            {
                boolean made = workDir.mkdirs();
                Log.i(TAG, "Made "+workDir+": "+made);
            }
            else
                Log.i(TAG, workDir+" exists");


            //make jetty/tmp
            File tmpDir = new File(jettyDir,__TMP_DIR);
            if (!tmpDir.exists())
            {
                boolean made = tmpDir.mkdirs();
                Log.i(TAG,"Made " + tmpDir + ": " + made);
            }
            else
            {
                Log.i(TAG,tmpDir + " exists");
            }

            //make jetty/webapps
            File webappsDir = new File(jettyDir,__WEBAPP_DIR);
            if (!webappsDir.exists())
            {
                boolean made = webappsDir.mkdirs();
                Log.i(TAG,"Made " + webappsDir + ": " + made);
            }
            else
            {
                Log.i(TAG,webappsDir + " exists");
            }

            //make jetty/etc
            File etcDir = new File(jettyDir,__ETC_DIR);
            if (!etcDir.exists())
            {
                boolean made = etcDir.mkdirs();
                Log.i(TAG,"Made " + etcDir + ": " + made);
            }
            else
            {
                Log.i(TAG,etcDir + " exists");
            }
            sendProgressUpdate(30);
            

            File webdefaults = new File(etcDir,"webdefault.xml");
            if (!webdefaults.exists() || updateNeeded)
            {
                //get the webdefaults.xml file out of resources
                try
                {
                    InputStream is = getResources().openRawResource(R.raw.webdefault);
                    OutputStream os = new FileOutputStream(webdefaults);
                    IO.copy(is,os);
                    Log.i(TAG,"Loaded webdefault.xml");
                }
                catch (Exception e)
                {
                    Log.e(TAG,"Error loading webdefault.xml",e);
                }
            }
            sendProgressUpdate(40);
            
            File realm = new File(etcDir,"realm.properties");
            if (!realm.exists() || updateNeeded)
            {
                try
                {
                    //get the realm.properties file out resources
                    InputStream is = getResources().openRawResource(R.raw.realm_properties);
                    OutputStream os = new FileOutputStream(realm);
                    IO.copy(is,os);
                    Log.i(TAG,"Loaded realm.properties");
                }
                catch (Exception e)
                {
                    Log.e(TAG,"Error loading realm.propeties",e);
                }
            }
            sendProgressUpdate(50);
            
            File keystore = new File(etcDir,"keystore");
            if (!keystore.exists() || updateNeeded)
            {
                try
                {
                    //get the keystore out of resources
                    InputStream is = getResources().openRawResource(R.raw.keystore);
                    OutputStream os = new FileOutputStream(keystore);
                    IO.copy(is,os);
                    Log.i(TAG,"Loaded keystore");
                }
                catch (Exception e)
                {
                    Log.e(TAG,"Error loading keystore",e);
                }
            }
            sendProgressUpdate(60);
            
            //make jetty/contexts
            File contextsDir = new File(jettyDir,__CONTEXTS_DIR);
            if (!contextsDir.exists())
            {
                boolean made = contextsDir.mkdirs();
                Log.i(TAG,"Made " + contextsDir + ": " + made);
            }
            else
            {
                Log.i(TAG,contextsDir + " exists");
            }
            sendProgressUpdate(70);
            
            //unpack the console war, but don't make a context.xml for it
            //Must be deployed by webapp deployer to get the Android ContentResolver
            //setting.
            File consoleWar = new File(webappsDir,"console");
            if (updateNeeded)
            {
                Installer.deleteWebapp(consoleWar);
                Log.i(TAG,"Cleaned console webapp for update");
            }

            boolean exists = consoleWar.exists();
            String[] files = consoleWar.list();
            if (!exists || (files == null) || (files.length == 0))
            {
                InputStream is = IJetty.this.getClassLoader().getResourceAsStream("console.war");
                Installer.install(is,"/console",webappsDir,"console",false);
                Log.i(TAG,"Loaded console webapp");
            }

            if (pi != null)
            {
                setStoredJettyVersion(pi.versionCode);
            }

            IJetty.this.setupState = __SETUP_DONE;
            sendProgressUpdate(100);
        }
    };
    
    static
    {
        __JETTY_DIR = new File(Environment.getExternalStorageDirectory(),"jetty"); 
    }
    
    public IJetty ()
    {
        super();
        handler = new Handler ()
        {
            public void handleMessage(Message msg) {
                int total = msg.getData().getInt("prog");
                progressDialog.setProgress(total);
                if (total >= 100){
                    dismissDialog(__SETUP_PROGRESS_DIALOG);
                }
            }
 
        };
    }

    public void consolePrint(String format, Object... args)
    {
        String msg = String.format(format,args);
        if (msg.length() > 0)
        {
            out.append(msg).append("<br/>");
            console.setText(Html.fromHtml(out.toString()));
            Log.i(TAG,msg); // Only interested in non-empty lines being output to Log
        }
        else
        {
            out.append(msg).append("<br/>");
            console.setText(Html.fromHtml(out.toString()));
        }

        if (scrollTask == null)
        {
            scrollTask = new ConsoleScrollTask();
        }

        consoleScroller.post(scrollTask);
    }

    protected int getStoredJettyVersion()
    {
        File jettyDir = __JETTY_DIR;
        if (!jettyDir.exists())
        {
            return -1;
        }
        File versionFile = new File(jettyDir,"version.code");
        if (!versionFile.exists())
        {
            return -1;
        }
        int val = -1;
        ObjectInputStream ois = null;
        try
        {
            ois = new ObjectInputStream(new FileInputStream(versionFile));
            val = ois.readInt();
            return val;
        }
        catch (Exception e)
        {
            Log.e(TAG,"Problem reading version.code",e);
            return -1;
        }
        finally
        {
            if (ois != null)
            {
                try
                {
                    ois.close();
                }
                catch (Exception e)
                {
                    Log.d(TAG,"Error closing version.code input stream",e);
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
       
        setContentView(R.layout.jetty_controller);

        // Watch for button clicks.
        final Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                if (isUpdateNeeded() && setupState != __SETUP_DONE)
                    IJettyToast.showQuickToast(IJetty.this,R.string.loading);
                else 
                {
                    printServerUrls();

                    //TODO get these values from editable UI elements
                    Intent intent = new Intent(IJetty.this,IJettyService.class);
                    intent.putExtra(__PORT,__PORT_DEFAULT);
                    intent.putExtra(__NIO,__NIO_DEFAULT);
                    intent.putExtra(__SSL,__SSL_DEFAULT);
                    intent.putExtra(__CONSOLE_PWD,__CONSOLE_PWD_DEFAULT);
                    startService(intent);
                }
            }
        });

        Button stopButton = (Button)findViewById(R.id.stop);
        stopButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                stopService(new Intent(IJetty.this,IJettyService.class));
            }
        });

        Button configButton = (Button)findViewById(R.id.config);
        configButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                IJettyEditor.show(IJetty.this);
            }
        });

        Button downloadButton = (Button)findViewById(R.id.download);
        downloadButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                IJettyDownloader.show(IJetty.this);
            }
        });

        console = (TextView)findViewById(R.id.console);
        consoleScroller = (ScrollView)findViewById(R.id.consoleScroller);

        try
        {
            pi = getPackageManager().getPackageInfo(getPackageName(),0);
            consolePrint("i-jetty version %s (%s)",pi.versionName,pi.versionCode);
        }
        catch (NameNotFoundException e)
        {
            consolePrint("Unable to determine running i-jetty version");
        }

        consolePrint("jetty server version %s",Server.UNKNOWN_VERSION);
        consolePrint("On %s",AndroidInfo.getDeviceModel());
        consolePrint("OS version %s",AndroidInfo.getOSVersion());
        consolePrint("");
        consolePrint("Project: http://code.google.com/p/i-jetty");
        consolePrint("Server: http://jetty.codehaus.org");
        consolePrint("Commercial Support: ");
        consolePrint("&nbsp;  http://www.intalioworks.com/services/webtide");
        consolePrint("&nbsp;  http://www.webtide.com/advice");
        consolePrint("");

        printNetworkInterfaces();
    }

    public static void show(Context context)
    {
        final Intent intent = new Intent(context,IJetty.class);
        context.startActivity(intent);
    }

    @Override
    protected void onResume()
    {
        if (!SdCardUnavailableActivity.isExternalStorageAvailable())
        {
            SdCardUnavailableActivity.show(this);
        }
        else 
        {
            //work out if we need to do the installation finish step
            //or not. We do it iff:
            // - there is no previous jetty version on disk
            // - the previous version does not match the current version
            // - we're not already doing the update
            if (setupState != __SETUP_DONE && setupState != __SETUP_RUNNING)
            {
                if (isUpdateNeeded())
                {
                    setupState = __SETUP_RUNNING;
                    setupJetty();
                }
            }
        }
        super.onResume();
    }
    
    

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch(id) 
        {
            case __SETUP_PROGRESS_DIALOG:
            {
                progressDialog = new ProgressDialog(IJetty.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("Finishing inital install ...");
                progressThread = new ProgressThread(handler);
                progressThread.start();
                return progressDialog;
            }
            default:
                return null;
        }
    }

    private void printNetworkInterfaces()
    {
        consolePrint("<b>Your Network Interfaces:</b>");
        try
        {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis))
            {
                Enumeration<InetAddress> iis = ni.getInetAddresses();
                for (InetAddress ia : Collections.list(iis))
                {
                    consolePrint("%s: %s",ni.getDisplayName(),ia.getHostAddress());
                    if (ia.getHostAddress().equals("10.0.2.15") && AndroidInfo.isOnEmulator(this))
                    {
                        consolePrint("<i>Running on Emulator</i>");
                        consolePrint("<i>Be sure you setup emulator port forwarding.<br/>http://bit.ly/adb-port-forwarding</i>");
                    }
                }
            }
        }
        catch (SocketException e)
        {
            consolePrint("Socket Exception: No Network Interfaces Available?");
        }
    }

    private void printServerUrls()
    {
        consolePrint("");
        consolePrint("<b>Server URLs:</b>");
        try
        {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis))
            {
                Enumeration<InetAddress> iis = ni.getInetAddresses();
                for (InetAddress ia : Collections.list(iis))
                {
                    StringBuffer url = new StringBuffer();
                    url.append("http://").append(ia.getHostAddress());
                    url.append(":").append(__PORT_DEFAULT);
                    url.append("/console");
                    if (ia.getHostAddress().equals("10.0.2.15") && AndroidInfo.isOnEmulator(this))
                    {
                        url.append("  <i>This URL only available on emulator itself");
                        url.append(", setup port forwarding to see i-jetty outside of emulator.");
                        url.append("<br/>http://bit.ly/adb-port-forwarding");
                        url.append("</i>");
                    }
                    consolePrint(url.toString());
                }
            }
        }
        catch (SocketException e)
        {
            consolePrint("Socket Exception: No Network Interfaces Available?");
        }
    }

    protected void setStoredJettyVersion(int version)
    {
        File jettyDir = __JETTY_DIR;
        if (!jettyDir.exists())
        {
            return;
        }
        File versionFile = new File(jettyDir,"version.code");
        ObjectOutputStream oos = null;
        try
        {
            FileOutputStream fos = new FileOutputStream(versionFile);
            oos = new ObjectOutputStream(fos);
            oos.writeInt(version);
            oos.flush();
        }
        catch (Exception e)
        {
            Log.e(TAG,"Problem writing jetty version",e);
        }
        finally
        {
            if (oos != null)
            {
                try
                {
                    oos.close();
                }
                catch (Exception e)
                {
                    Log.d(TAG,"Error closing version.code output stream",e);
                }
            }
        }
    }
  
    /**
     * We need to an update iff we don't know the current
     * jetty version or it is different to the last version
     * that was installed.
     * 
     * @return
     */
    public boolean isUpdateNeeded ()
    {        
        int storedVersion = getStoredJettyVersion();
        if (storedVersion <= 0)
            return true;

        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(),0);
            if (pi == null)
                return true;
            if (pi.versionCode != storedVersion)
                return true;
        }
        catch (Exception e)
        {
            return true;
        }

        return false;
    }

    public void setupJetty()
    {
        showDialog(__SETUP_PROGRESS_DIALOG);        
    };

}
