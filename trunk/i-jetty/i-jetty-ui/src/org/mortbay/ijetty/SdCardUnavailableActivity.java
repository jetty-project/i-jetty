package org.mortbay.ijetty;

import org.mortbay.ijetty.util.IJettyToast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SdCardUnavailableActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_sd_unavailable);

        refreshSDState();

        Button btn = (Button)findViewById(R.id.retry);
        btn.setOnClickListener(new Button.OnClickListener()
        {
            public void onClick(View v)
            {
                doRetry(v);
            }
        });
    }
    
    public static void show(Context context) {
        final Intent intent = new Intent(context, SdCardUnavailableActivity.class);
        context.startActivity(intent);
    }
    
    public void doRetry(View v) {
        if(isExternalStorageAvailable()) {
            IJetty.show(this);
            return;
        }
        
        IJettyToast.showQuickToast(this,R.string.sd_not_available);
        refreshSDState();
    }
    
    public static boolean isExternalStorageAvailable()
    {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private void refreshSDState()
    {
        TextView message = (TextView)findViewById(R.id.message);
        String header = getString(R.string.header_sd_media) + "\n\n";

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_REMOVED.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_removed));
            return;
        }
        if (Environment.MEDIA_UNMOUNTED.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_unmounted));
            return;
        }

        if (Environment.MEDIA_CHECKING.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_checking));
            return;
        }

        if (Environment.MEDIA_NOFS.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_nofs));
            return;
        }

        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_mounted_read_only));
            return;
        }

        if (Environment.MEDIA_SHARED.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_shared));
            return;
        }

        if (Environment.MEDIA_BAD_REMOVAL.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_bad_removal));
            return;
        }

        if (Environment.MEDIA_UNMOUNTABLE.equals(state))
        {
            message.setText(header + getString(R.string.sd_media_unmountable));
            return;
        }

        message.setText(header + getString(R.string.sd_unknown,state));
    }
}
