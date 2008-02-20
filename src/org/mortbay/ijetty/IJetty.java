package org.mortbay.ijetty;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class IJetty extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.jetty_controller);

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.start);
        button.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        // Make sure the service is started.  It will continue running
                        // until someone calls stopService().
                        startService(new Intent(IJetty.this,
                                IJettyService.class), null);
                    }
                }
        );
        button = (Button)findViewById(R.id.stop);
        button.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        // Cancel a previous call to startService().  Note that the
                        // service will not actually stop at this point if there are
                        // still bound clients.
                        stopService(new Intent(IJetty.this,
                                IJettyService.class));
                    }
                }
        );
    }

}