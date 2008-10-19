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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;



/**
 * IJettyDownloader
 *
 * Download and install new webapp
 */
public class IJettyDownloader extends Activity
{
    private HttpClient client = new HttpClient();
    private File tmpDir;
    
    
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        tmpDir = new File("/sdcard/tmp");
        if (!tmpDir.exists())
            tmpDir.mkdirs();
        
        setContentView(R.layout.jetty_downloader);
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
    
    
    public void startDownload (String url)
    {
        final String war = getWarFileName (url);
        final File warFile = new File (tmpDir, war);
        if (warFile.exists())
        {
            //TODO something better
            Log.i("Jetty", "File exists");
            return;
        }
        
        ContentExchange exchange = new ContentExchange()
        {
            private OutputStream _outputStream;

            protected void onResponseComplete() throws IOException
            {  
                closeOutputStream();
                if (getStatus() == HttpStatus.ORDINAL_200_OK)
                    install (warFile);
                else
                    Log.e("Jetty", "Bad status: "+getStatus());
            }  
         
            
            protected void onConnectionFailed(Throwable ex)
            {
                closeOutputStream();
                //Show error message
                super.onConnectionFailed(ex);
            }

            protected void onException(Throwable ex)
            {
                closeOutputStream();
                //show error message
                super.onException(ex);
            }

            protected void onExpire()
            {
                closeOutputStream();
                //show error message
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
                }
            }
        };
        
        exchange.setURL(url);
        try
        {
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
           URL canonicalUrl = new URL(url);
           return canonicalUrl.getFile();
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
            
            Resource war = Resource.newResource(file.getCanonicalPath());
            JarResource.extract(war, webapp, false);
            
            //TODO message success
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Bad resource", e);
        }

    }
}
