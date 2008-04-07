package org.mortbay.ijetty;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class IJetty extends Activity 
{
	private IPList _ipList;
	
	
	private class IPList 
	{
		private List _list = new ArrayList();
		
		public IPList()
		{
		}
		
		public int getCount ()
		{
			return _list.size();
		}
		
		public String getItem(int index)
		{
			return (String)_list.get(index);
		}
		
		public void refresh ()
		{
			_list.clear();
		
			try
			{
				Enumeration nis = NetworkInterface.getNetworkInterfaces();
				while (nis.hasMoreElements())
				{
					NetworkInterface ni = (NetworkInterface)nis.nextElement();
					Enumeration iis = ni.getInetAddresses();
					while (iis.hasMoreElements())
					{
						_list.add(ni.getDisplayName()+": "+((InetAddress)iis.nextElement()).getHostAddress());
					}
				}
			}
			catch (Exception e)
			{
				Log.e("JETTY", "Problem retrieving ip addresses", e);
			}
		}
	}
	
    private class NetworkListAdapter extends BaseAdapter 
    {
    	private Context _context;
    	private IPList _ipList;
    	
        public NetworkListAdapter(Context context, IPList ipList) 
        {
            _context = context;
            _ipList = ipList;
            _ipList.refresh();
        }

        public int getCount() 
        {
            return _ipList.getCount();
        }

        public boolean areAllItemsSelectable() 
        {
            return false;
        }

        public boolean isSelectable(int position) 
        {
            return false;
        }

        public Object getItem(int position) 
        {
            return position;
        }

        public long getItemId(int position) 
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) 
        {
            TextView tv;
            if (convertView == null) 
            {
                tv = new TextView(_context);
            } 
            else 
            {
                tv = (TextView) convertView;
            }
            tv.setText(_ipList.getItem(position));
            return tv;
        }
    }

	
	
	
	
	
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle icicle) 
    {
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
        
        ListView list = (ListView) findViewById(R.id.list);
        _ipList = new IPList();
        list.setAdapter(new NetworkListAdapter(this, _ipList));

    }


	protected void onResume()
	{
		_ipList.refresh();
		super.onResume();
	}
    
    

}