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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.mortbay.util.IO;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * IJetty
 *
 * Main Jetty activity.
 * Can start other activities:
 *   + configure
 *   + download
 *   
 *  Can start/stop services:
 *   + IJettyService
 */
public class IJetty extends Activity 
{
    public static final String __PORT = "org.mortbay.ijetty.port";
    public static final String __NIO = "org.mortbay.ijetty.nio";
    public static final String __CONSOLE_PWD = "org.mortbay.ijetty.console";
    
    public static final String __PORT_DEFAULT = "8080";
    public static final boolean __NIO_DEFAULT = true;
    public static final String __CONSOLE_PWD_DEFAULT = "admin";
    
    public static final String __JETTY_DIR = "/sdcard/jetty";
    public static final String __WEBAPP_DIR = "webapps";
    public static final String __ETC_DIR = "etc";
    public static final String __CONTEXTS_DIR = "contexts";
    public static final String __TMP_DIR = "tmp";
    
    private IPList _ipList;


    private class IPList 
    {
        private List _list = new ArrayList();

        public IPList()
        {
        }

        public int getCount ()
        {
            return _list.size();
        }

        public String getItem(int index)
        {
            return (String)_list.get(index);
        }

        public void refresh ()
        {
            _list.clear();

            try
            {
                Enumeration nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements())
                {
                    NetworkInterface ni = (NetworkInterface)nis.nextElement();
                    Enumeration iis = ni.getInetAddresses();
                    while (iis.hasMoreElements())
                    {
                        _list.add(ni.getDisplayName()+": "+((InetAddress)iis.nextElement()).getHostAddress());
                    }
                }
            }
            catch (Exception e)
            {
                Log.e("JETTY", "Problem retrieving ip addresses", e);
            }
        }
    }

    private class NetworkListAdapter extends BaseAdapter 
    {
        private Context _context;
        private IPList _ipList;

        public NetworkListAdapter(Context context, IPList ipList) 
        {
            _context = context;
            _ipList = ipList;
            _ipList.refresh();
        }

        public int getCount() 
        {
            return _ipList.getCount();
        }

        public boolean areAllItemsSelectable() 
        {
            return false;
        }

        public boolean isSelectable(int position) 
        {
            return false;
        }

        public Object getItem(int position) 
        {
            return position;
        }

        public long getItemId(int position) 
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) 
        {
            TextView tv;
            if (convertView == null) 
            {
                tv = new TextView(_context);
            } 
            else 
            {
                tv = (TextView) convertView;
            }
            tv.setText(_ipList.getItem(position));
            return tv;
        }
    }




    /** Called when the activity is first created. */
    public void onCreate(Bundle icicle) 
    {
        setupJetty();
        super.onCreate(icicle);
        setContentView(R.layout.jetty_controller);

        // Watch for button clicks.
        final Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {  
                        //TODO get these values from editable UI elements
                        Intent intent = new Intent(IJetty.this, IJettyService.class);
                        intent.putExtra(__PORT, __PORT_DEFAULT);
                        intent.putExtra(__NIO, __NIO_DEFAULT);
                        intent.putExtra(__CONSOLE_PWD, __CONSOLE_PWD_DEFAULT);
                        startService(intent);
                    }
                }
        );

        Button stopButton = (Button)findViewById(R.id.stop);
        stopButton.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        stopService(new Intent(IJetty.this, IJettyService.class));
                    }
                }
        );

        Button configButton = (Button)findViewById(R.id.config);
        configButton.setOnClickListener(
            new OnClickListener()
            {
              public void onClick(View v)
              {
                startActivity(new Intent(IJetty.this, IJettyEditor.class));
              }
            }
        );
        
        Button downloadButton = (Button)findViewById(R.id.download);
        downloadButton.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        startActivity(new Intent(IJetty.this, IJettyDownloader.class));
                    }
                }
        );

        ListView list = (ListView) findViewById(R.id.list);
        _ipList = new IPList();
        list.setAdapter(new NetworkListAdapter(this, _ipList));

    }

    protected void onResume()
    {
        _ipList.refresh();
        super.onResume();
    }
    
    
    public void setupJetty ()
    {
        //create the jetty dir structure
        File jettyDir = new File(__JETTY_DIR);
        if (!jettyDir.exists())
            jettyDir.mkdirs();
        
        File webappsDir = new File (jettyDir, __WEBAPP_DIR);
        if (!webappsDir.exists())
        {
            webappsDir.mkdirs();           
        }

        File etcDir = new File (jettyDir, __ETC_DIR);
        if (!etcDir.exists())
            etcDir.mkdirs();

        File webdefaults = new File (etcDir, "webdefault.xml");
        if (!webdefaults.exists())
        {
            //get the webdefaults.xml file out of resources
            try
            {
                InputStream is = getResources().openRawResource(R.raw.webdefault);
                OutputStream os = new FileOutputStream(webdefaults);
                IO.copy(is, os);
                Log.i("Jetty", "Loaded webdefault.xml");
            }
            catch (Exception e)
            {
                Log.e("Jetty", "Error loading webdefault.xml", e);
            }
        }
        File realm = new File (etcDir, "realm.properties"); 
        if (!realm.exists())
        {
            try
            {
                //get the realm.properties file out resources
                InputStream is = getResources().openRawResource(R.raw.realm_properties);
                OutputStream os = new FileOutputStream(realm);
                IO.copy(is,os);
                Log.i("Jetty", "Loaded realm.properties");
            }
            catch (Exception e)
            {
                Log.e("Jetty", "Error loading realm.propeties", e);
            }
        }

        File contextsDir = new File (jettyDir, __CONTEXTS_DIR);
        if (!contextsDir.exists())
            contextsDir.mkdirs();

        //unpack the console war
        File consoleWar = new File (webappsDir, "console");
        boolean exists = consoleWar.exists();
        String[] files = consoleWar.list();
        
        if (!exists || files == null || files.length == 0)
        {
            InputStream is = this.getClassLoader().getResourceAsStream("console.war");
            Installer.install(is, "/console", webappsDir, "console");
            /*
            
            if (is != null)
            {
                try
                {
                    JarInputStream jin = new JarInputStream(is);
                    JarEntry entry;
                    while((entry=jin.getNextJarEntry())!=null)
                    {
                        String entryName = entry.getName();             
                        File file=new File(consoleWar,entryName);
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
                }
                catch (Exception e)
                {
                    Log.e("Jetty", "Error inflating console.war", e);
                }
            }
            else
                Log.e("Jetty", "No console war");
                */
        }
    }
}
