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


package org.mortbay.ijetty.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.mortbay.util.IO;

import android.util.Log;

public abstract class AndroidResource
{

        public abstract boolean exists();
        public abstract boolean isDirectory();
        public abstract InputStream getInputStream() throws java.io.IOException;
        public abstract OutputStream getOutputStream() throws java.io.IOException, SecurityException;
        
        public void writeTo (OutputStream out)
        throws IOException
        {
            InputStream is = getInputStream();
            IO.copy(is, out);
        }
        
        public static AndroidResource getResource (String path)
        throws MalformedURLException
        {
            Log.i("Jetty", "Getting resource for path="+path);
            /*
            if (path.startsWith("contacts"))
            {
                //return new AndroidContactResource
                return null;
            }
            else if (path.startsWith("settings"))
            {
                //return new AndroidSettingsResource
                return null;
            }
            else 
            */
            return new AndroidFileResource(new URL(path));
        }
}
