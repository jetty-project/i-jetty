package org.mortbay.ijetty.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public final class AndroidInfo
{
    private static final String EMULATOR_ID = "ffffffffffffffff";

    public static CharSequence getApplicationLabel(Context context)
    {
        try
        {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(),0);
            return pm.getApplicationLabel(ai);
        }
        catch (NameNotFoundException e)
        {
            return "AnonDroid";
        }
    }

    public static String getApplicationVersion(Context context)
    {
        try
        {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(context.getPackageName(),0).versionName;
        }
        catch (NameNotFoundException e)
        {
            return "";
        }
    }

    public static boolean isOnEmulator(Context context)
    {
        if ("sdk".equals(Build.MODEL) && "sdk".equals(Build.PRODUCT))
        {
            return true;
        }

        return getUniqueDeviceID(context).equals(EMULATOR_ID);
    }

    public static String getDeviceModel()
    {
        StringBuilder ret = new StringBuilder();

        if ("sdk".equals(Build.MODEL) && "sdk".equals(Build.PRODUCT))
        {
            return "SDK Emulator";
        }

        ret.append(Build.MODEL).append(" [");
        ret.append(Build.MANUFACTURER).append(" ");
        ret.append(Build.PRODUCT).append("]");

        return ret.toString();
    }

    public static String getOSVersion()
    {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.CUR_DEVELOPMENT)
        {
            return "DEV";
        }
        return Build.VERSION.RELEASE;
    }

    public static String getUniqueDeviceID(Context context)
    {
        ContentResolver contentResolver = context.getContentResolver();
        String id = android.provider.Settings.System.getString(contentResolver,android.provider.Settings.System.ANDROID_ID);
        if (id == null)
        {
            id = EMULATOR_ID; // running on emulator.
        }
        return id;
    }

    /**
     * Build an HTTP User-Agent suitable enough to identify this application + version + handset
     */
    public static String getUserAgent(Context context)
    {
        StringBuilder ua = new StringBuilder();
        ua.append(AndroidInfo.getApplicationLabel(context)).append("/");
        ua.append(AndroidInfo.getApplicationVersion(context));
        ua.append(" (Android ").append(AndroidInfo.getOSVersion());
        ua.append("/").append(AndroidInfo.getDeviceModel()).append(")");
        return ua.toString();
    }

    private AndroidInfo()
    {
        /* prevent instantiation */
    }
}
