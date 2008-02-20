package org.mortbay.ijetty;

import org.mortbay.log.Logger;

import android.util.Log;

public class AndroidLog implements Logger
{
    public static final String __JETTY_TAG = "Jetty";
    
    
    public AndroidLog()
    {
        this ("org.mortbay.log");
    }
    
    public AndroidLog(String name)
    {     
    }
    
    public void debug(String msg, Throwable th)
    {
        Log.d(__JETTY_TAG, msg, th);
    }

    public void debug(String msg, Object arg0, Object arg1)
    {
        Log.d(__JETTY_TAG, msg);
    }

    public Logger getLogger(String name)
    {
       return new AndroidLog(name);
    }

    public void info(String msg, Object arg0, Object arg1)
    {
        Log.i(__JETTY_TAG, msg);
    }

    public boolean isDebugEnabled()
    {
        return Log.isLoggable(__JETTY_TAG, Log.DEBUG);
    }

    public void setDebugEnabled(boolean enabled)
    {
        //not supported by android logger
    }

    public void warn(String msg, Object arg0, Object arg1)
    {
        Log.w(__JETTY_TAG, msg);
    }

    public void warn(String msg, Throwable th)
    {
        Log.e(__JETTY_TAG, msg, th);
    }

}
