package org.mortbay.ijetty.console;

import android.os.Build;
import android.util.Log;

public class AndroidInfo
{
    public static boolean isOldContactsSystem()
    {
        Log.i("AndroidInfo","isOldContactsSystem() | sdk_int=" + Build.VERSION.SDK_INT + " | " + (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT));
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.CUR_DEVELOPMENT)
        {
            return true;
        }
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT;
    }
}
