package org.mortbay.ijetty.console;

import android.os.Build;

public class AndroidInfo
{
    public static boolean isOldContactsSystem()
    {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.CUR_DEVELOPMENT)
        {
            return true;
        }
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT;
    }
}
