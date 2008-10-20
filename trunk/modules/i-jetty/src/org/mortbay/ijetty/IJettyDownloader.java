//========================================================================
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
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.mortbay.resource.JarResource;
import org.mortbay.resource.Resource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



/**
 * IJettyDownloader
 *
 * Download and install new webapp
 */
public class IJettyDownloader extends Activity
{
    public static final int __MSG_DOWNLOAD_SUCCEEDED = 0;
    public static final int __MSG_DOWNLOAD_FAILED = 1;
    public static final int __MSG_PROGRESS = 2;
    
    private HttpClient client = new HttpClient();
    private File tmpDir;
    private ProgressBar _progressBar;
    
    private final Handler mHandler = new Handler() 
    {
        public void handleMessage(Message msg) 
        {
            switch (msg.what) 
            {
                case __MSG_DOWNLOAD_SUCCEEDED:
                { 
                    _progressBar.setProgress(100);
                    _progressBar.setVisibility(ProgressBar.INVISIBLE);
                    ((TextView)findViewById(R.id.loading)).setVisibility(TextView.INVISIBLE);
                    ((EditText)findViewById(R.id.download_url)).setText("");
                    AlertDialog.Builder builder = new AlertDialog.Builder(IJettyDownloader.this);
                    builder.setCancelable(true);
                    builder.setMessage(R.string.download_success);
                    builder.setTitle(R.string.success);
                    builder.show();
                    break;
                }
                case __MSG_DOWNLOAD_FAILED:
                {
                    _progressBar.setProgress(100);
                    _progressBar.setVisibility(ProgressBar.INVISIBLE);
                    ((TextView)findViewById(R.id.loading)).setVisibility(TextView.INVISIBLE);
                    AlertDialog.Builder builder = new AlertDialog.Builder(IJettyDownloader.this);
                    builder.setCancelable(true);
                    builder.setMessage((String)msg.obj);
                    builder.setTitle(R.string.download_fail);
                    builder.show();
                    break;
                }
                case __MSG_PROGRESS:
                {
                    //onReportProgress(msg.arg1);
                    break;
                }
                default:
                    Log.e("Jetty", "Unknown message id "+ msg.what);
            }
        }
    };

    
    
    /**
     * IJettyDownloader
     */
    public IJettyDownloader()
    {
        client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
        if (client != null)
        {
            try
            {
                client.start();
            }
            catch (Exception e)
            {
                Log.e("Jetty", "Error starting client", e);
            }
        }
    }
    
    /** 
     * onCreate
     * 
     * Create the download activity.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        tmpDir = new File("/sdcard/jetty/tmp/");
        if (!tmpDir.exists())
            tmpDir.mkdirs();
        
        setContentView(R.layout.jetty_downloader);
        _progressBar = (ProgressBar)findViewById(R.id.progress);
        final Button startDownloadButton = (Button)findViewById(R.id.start_download);
        startDownloadButton.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {  
                        EditText text = (EditText) findViewById(R.id.download_url);
                        String url = text.getText().toString();
                        if (url==null)
                            return;
                        if ("".equals(url.trim()))
                            return;

                        startDownload(url);
                    }
                }
        );
    }
    
    
    /**
     * Begin a download of a war file.
     * @param url
     */
    public void startDownload (final String url)
    {
        final String war = getWarFileName (url);
        final File warFile = new File (tmpDir, war);
        try
        {
            if (!warFile.createNewFile())
            {
                Log.i("Jetty", "File exists");
               // Toast.makeText(IJettyDownloader.this, R.string.webapp_exists,Toast.LENGTH_SHORT).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true);
                builder.setMessage(R.string.overwrite);
                builder.setTitle(R.string.webapp_exists);
                builder.setPositiveButton(R.string.yes, 
                                          new DialogInterface.OnClickListener (){
                                            public void onClick(
                                                    DialogInterface arg0,
                                                    int arg1)
                                            {
                                                cleanDownload(warFile);
                                                doDownload(url, warFile);
                                            }});
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
                {
                    public void onClick(
                            DialogInterface arg0,
                            int arg1)
                    {
                    }
                });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    public void onCancel(DialogInterface arg0)
                    {                       
                    }
                });
                builder.show();
            }
            else
                doDownload(url, warFile);
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error creating file "+war, e);
            return;
        }
    }
    
    /**
     * Download and install the file as a webapp.
     * 
     * @param url
     * @param warFile
     */
    public void doDownload(final String url, final File warFile)
    {
        //Get the file    
        _progressBar.setVisibility(ProgressBar.VISIBLE);
        _progressBar.setProgress(0);
        _progressBar.setIndeterminate(true);
        ((TextView)findViewById(R.id.loading)).setVisibility(TextView.VISIBLE);
        
        ContentExchange exchange = new ContentExchange()
        {
            private OutputStream _outputStream;

            protected void onResponseComplete() throws IOException
            {  
                closeOutputStream();
                if (getResponseStatus() == HttpStatus.ORDINAL_200_OK)
                    install (warFile);
                else
                {
                    Log.e("Jetty", "Bad status: "+getResponseStatus());                 
                    mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Bad status: "+getResponseStatus()));
                }
            }  
         
            protected void onConnectionFailed(Throwable ex)
            {
                closeOutputStream();
                mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Connection failed"));
                Log.e("Jetty", "Connection fail", ex);
                super.onConnectionFailed(ex);
            }

            protected void onException(Throwable ex)
            {
                closeOutputStream();
                mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Exception"));
                Log.e("Jetty", "Error on download", ex);
                super.onException(ex);
            }

            protected void onExpire()
            {
                closeOutputStream(); 
                mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Expired"));
                Log.e("Jetty", "Expired: "+url);
                super.onExpire();
            }

            protected void onResponseContent( Buffer content )
            throws IOException
            {
                try
                {
                    OutputStream os = getOutputStream();
                    content.writeTo( os );
                }
                catch ( Exception e )
                {
                    Log.e("Jetty", "Error reading content", e);
                    mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Exception"));
                }
            }

            protected OutputStream getOutputStream()
            throws IOException
            {
                if ( _outputStream == null )
                {
                    _outputStream = new FileOutputStream (warFile);
                }
                return _outputStream;
            }

            protected void closeOutputStream()
            { 
                try
                {
                    if (_outputStream != null)
                        _outputStream.close();
                }
                catch (IOException e)
                {
                    Log.e("Jetty", "Error closing stream", e);
                    mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Exception"));
                }
            }
        };
        
        exchange.setURL(url);
        try
        {
            Log.i("Jetty", "Downloading "+url);
            client.send(exchange);
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Download failed for "+url);
        }
    }

    public String getWarFileName (String url)
    {
        if (url == null)
            return null;
        
        try
        {
            int dot = url.lastIndexOf("?");
            String war = (dot < 0 ? url : url.substring(0,dot));
            dot = url.lastIndexOf('/');
            war = (dot < 0 ? url : url.substring(dot+1));
            return war;
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Bad url "+url);
            return null;
        }
    }
    
    public void install (File file)
    {
        try
        {
            File webappDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__WEBAPP_DIR);
            String name = file.getName();
            if (name.endsWith(".war") || name.endsWith(".jar"))
                name = name.substring(0, name.length()-4);
            
            File webapp = new File (webappDir, name);
            if (!webapp.exists())
                webapp.mkdirs();
            
            Resource war = Resource.newResource("jar:"+file.toURL()+"!/");
            JarResource.extract(war, webapp, false);
            mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_SUCCEEDED));
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Bad resource", e);
            mHandler.sendMessage(Message.obtain(mHandler, __MSG_DOWNLOAD_FAILED, "Exception"));
        }
    }
    
    public void cleanDownload (File file)
    {
        File webappDir = new File (IJetty.__JETTY_DIR+"/"+IJetty.__WEBAPP_DIR);
        String name = file.getName();
        if (name.endsWith(".war") || name.endsWith(".jar"))
            name = name.substring(0, name.length()-4);

        File webapp = new File (webappDir, name);
        if (webapp.exists())
            deleteWebapp(webapp);

        file.delete();
    }

    public void deleteWebapp (File webapp)
    {
        if (webapp.isDirectory())
        {
            File[] files = webapp.listFiles();
            for (File f:files)
            {
                deleteWebapp(f);
            }
        }
        else
            webapp.delete();
    }
}
