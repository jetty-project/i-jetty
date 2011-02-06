package org.mortbay.ijetty.util;

import org.mortbay.ijetty.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class IJettyToast
{
    public static void showServiceToast(Context context, int messageId)
    {
        showToast(context,messageId,Toast.LENGTH_LONG);
    }

    private static void showToast(Context context, int messageId, int duration)
    {
        final View view = LayoutInflater.from(context).inflate(R.layout.service_toast,null);
        ((TextView)view.findViewById(R.id.message)).setText(messageId);

        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setView(view);

        toast.show();
    }

    public static void showQuickToast(Context context, int messageId)
    {
        showToast(context,messageId,Toast.LENGTH_SHORT);
    }
}
