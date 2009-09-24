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

import org.mortbay.log.Logger;

import android.util.Log;

public class AndroidLog implements Logger
{
    private final String name;
    private volatile boolean debugEnabled;

    public AndroidLog(String name)
    {
        this.name = name;
    }

    public boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled)
    {
        this.debugEnabled = debugEnabled;
    }

    public void info(String message)
    {
        Log.i(name, message);
    }

    public void info(String message, Object arg1, Object arg2)
    {
        Log.i(name, format(message, arg1, arg2));
    }

    public void debug(String message)
    {
        if (isDebugEnabled())
            Log.d(name, message);
    }

    public void debug(String message, Throwable throwable)
    {
        if (isDebugEnabled())
            Log.d(name, message, throwable);
    }

    public void debug(String message, Object arg1, Object arg2)
    {
        if (isDebugEnabled())
            Log.d(name, format(message, arg1, arg2));
    }

    public void warn(String message)
    {
        Log.w(name, message);
    }

    public void warn(String message, Object arg1, Object arg2)
    {
        Log.w(name, format(message, arg1, arg2));
    }

    public void warn(String message, Throwable throwable)
    {
        Log.w(name, message, throwable);
    }

    public Logger getLogger(String name)
    {
        return new AndroidLog(name);
    }

    public String getName()
    {
        return name;
    }

    private String format(String format, Object arg1, Object arg2)
    {
        if (format == null)
            return "";
        String[] formats = format.split("\\{\\}", -1);
        if (formats.length == 1)
            return formats[0]; // Means the format string did not have {}

        StringBuilder builder = new StringBuilder();

        if (formats.length == 2)
            // Only one {}
            return builder.append(formats[0]).append(arg1).append(formats[1]).toString();

        if (formats.length == 3)
             // Two {}
            return builder.append(formats[0]).append(arg1).append(formats[1]).append(arg2).append(formats[2]).toString();

        // Should not happen, but handle it anyway
        builder.append(formats[0]).append(arg1).append(formats[1]).append(arg2).append(formats[2]);
        for (int i = 3; i < formats.length; ++i)
            builder.append(formats[i]).append("{}");
        return builder.toString();
    }
}
