package org.mortbay.ijetty.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        {
            Log.i("Jetty", "Getting resource for path="+path);
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
                return new AndroidFileResource(path);
        }
}
