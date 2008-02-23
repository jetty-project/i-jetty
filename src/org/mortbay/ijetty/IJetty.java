package org.mortbay.ijetty;


import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class IJetty extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.jetty_controller);
       
        // Watch for button clicks.
        final Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {  
                        SharedPreferences prefs = getSharedPreferences("jetty", MODE_WORLD_WRITEABLE);
                        if (prefs!=null)
                        {
                            try
                            {
                                Map m = prefs.getAll();
                                Iterator itor = m.entrySet().iterator();
                                while (itor.hasNext())
                                {
                                    Map.Entry me = (Map.Entry)itor.next();
                                    Log.i("Jetty", "Map entry "+me.getKey()+":"+me.getValue());
                                }
                            }
                            catch (NullPointerException e)
                            {
                                Log.e("Jetty", "No preferences");
                            }

                        }
                        else
                            Log.i("Jetty", "there are no preferences");
                        //boolean isRunning = prefs.getBoolean("isRunning", false);
                        //Log.i("Jetty", "is running: "+isRunning);
                        //if (isRunning)
                        //    Toast.makeText(IJetty.this, getText(R.string.jetty_started), Toast.LENGTH_SHORT).show();
                        //else
                            startService(new Intent(IJetty.this, IJettyService.class), null);

                    }
                }
        );
        
        Button stopButton = (Button)findViewById(R.id.stop);
        stopButton.setOnClickListener(
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        //SharedPreferences prefs = getSharedPreferences("jetty", MODE_WORLD_WRITEABLE);
                        //boolean isRunning = prefs.getBoolean("isRunning", false);
                        //if (!isRunning)
                        //    Toast.makeText(IJetty.this, getText(R.string.jetty_stopped), Toast.LENGTH_SHORT).show();
                        //else
                        //Log.i("Jetty", "is running: "+isRunning);
                            stopService(new Intent(IJetty.this, IJettyService.class));
                        
                    }
                }
        );
    }

}