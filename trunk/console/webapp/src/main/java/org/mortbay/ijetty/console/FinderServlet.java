package org.mortbay.ijetty.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentResolver;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class FinderServlet extends HttpServlet
{
    
    private static final String TAG = "IJetty.Cnsl";
    public static final String LAST_NETWORK_LOCATION = "org.mortbay.ijetty.console.lastNetworkLocation";
    public static final String LAST_GPS_LOCATION = "org.mortbay.ijetty.console.lastGPSLocation";
    public static final String NETWORK_LISTENER = "org.mortbay.ijetty.console.networkListener";
    public static final String GPS_LISTENER = "org.mortbay.ijetty.console.gpsListener";
    public static final String LAST_TRACKER_REQUEST_TIME = "org.mortbay.ijetty.console.lastTrackerRequestTime";
    public static final long CONTINUATION_TIMEOUT = 5000L; //wait up to 5sec to get a location when enabling tracking
    public static final long INTER_REQUEST_TIMEOUT = 1000L * 60 * 5; //10 mins without requests
    public static final int DEFAULT_RING_SEC = 10;
    android.content.Context androidContext;
    ContentResolver resolver;
    LocationManager locationManager;
    public Map<String,Location> providerLocations = Collections.synchronizedMap(new HashMap<String,Location>());
    public Object sync = new Object();
    LooperThread gpsLooper;
    LooperThread networkLooper;
    Thread controlThread;
    AtomicBoolean tracking = new AtomicBoolean();
    MediaListener mediaListener;
    MediaPlayer mediaPlayer;
    
    
    class StopperThread extends Thread
    {
       
        public void run ()
        {
            try
            {
                Thread.currentThread().sleep(10000);
                if (mediaPlayer != null)
                    mediaPlayer.stop();          
            }
            catch (InterruptedException e)
            {
                Log.i(TAG, "Interruped waiting to stop mediaplayer ringtone");
            }
            finally
            {
                //release the resources
                if (mediaPlayer != null)
                {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        }
    }
    
    class MediaListener implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener
    {

        public void onPrepared(MediaPlayer mp)
        {
            mp.start();
            StopperThread stopper = new StopperThread();
            stopper.start();
        }

        public void onCompletion(MediaPlayer mp)
        {
            mp.stop();
        }

        public boolean onError(MediaPlayer mp, int what, int extra)
        {
            mp.stop();
            return false;
        }

    }

    class LooperThread extends Thread 
    {
        String provider;
        Looper looper;
        AsyncLocationListener listener;
        
        public LooperThread (String provider)
        {
            super();
            this.provider = provider;
            setDaemon(true);
        }
        
        public void run() 
        {
            Looper.prepare();
            looper = Looper.myLooper();
            listener = new AsyncLocationListener(provider);
            locationManager.requestLocationUpdates(provider, 60000L, 0F, listener, looper); //Get an update every 60 secs
            Log.d(TAG, "Requested location updates for "+provider);
            Looper.loop();
        }


        public void quit ()
        {
            locationManager.removeUpdates(listener);
            looper.quit();
        }
    }
    
    
    
    class ControlThread extends Thread
    {
        public ControlThread()
        {
            super("Looper-Control");
            setDaemon(true);
        }
        
        public void run()
        {
            try
            {
                while (true)
                {
                    checkInterRequestGap();
                    Thread.currentThread().sleep(60000);
                }
            }
            catch (InterruptedException e)
            {
                //Finish loop
                return;
            }
        }
    }
    
    public class AsyncLocationListener implements LocationListener
    {
        String provider = null;
        
        public AsyncLocationListener (String provider)
        {
            this.provider = provider;
        }


        public void onLocationChanged(Location location)
        {
            Log.d(TAG, "Provider: "+provider+" location change: "+location);
            synchronized (sync)
            {
                try 
                {
                    providerLocations.put(provider, location);
                    Log.d(TAG, "put location");
                    sync.notifyAll();
                    Log.d(TAG, "notified All");
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            // TODO Auto-generated method stub
            
        }

        public void onProviderEnabled(String provider)
        {
            // TODO Auto-generated method stub
            
        }

        public void onProviderDisabled(String provider)
        {
            //unregister the listener?
        }
        
    }
    
    
    
    
    
    public ContentResolver getContentResolver()
    {
        return resolver;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
        androidContext = (android.content.Context)config.getServletContext().getAttribute("org.mortbay.ijetty.context");
        locationManager = (LocationManager)androidContext.getSystemService(Context.LOCATION_SERVICE);
        controlThread = new ControlThread();
        controlThread.start();
    }


    public void startTrackers()
    {
        synchronized (sync)
        {
            tracking.set(true);
            if (gpsLooper == null)
            {
                gpsLooper = new LooperThread(LocationManager.GPS_PROVIDER);
            }
            if (networkLooper == null)
            {
                networkLooper = new LooperThread(LocationManager.NETWORK_PROVIDER);
            }
        }
      
        if (!gpsLooper.isAlive())
        {
            Log.i(TAG, "Starting gps looper thread");
            gpsLooper.start();
            Log.i(TAG, "Started gps looper thread");
        }
        
        
        if (!networkLooper.isAlive())
        { 
            Log.i(TAG, "Starting network looper thread");
            networkLooper.start();
            Log.i(TAG, "Started network looper thread");
        }
    }
    
    
    public void stopTrackers()
    {
        Log.d(TAG, "Stopping trackers");
        synchronized (sync)
        { 
            tracking.set(false);
            if (networkLooper != null)
            {
                Log.d(TAG, "Stopping network looper");
                ((LooperThread)networkLooper).quit();
                networkLooper.interrupt();
                networkLooper = null;
            }

            if (gpsLooper != null)
            {
                Log.d(TAG, "Stopping gps looper");
                ((LooperThread)gpsLooper).quit();
                gpsLooper.interrupt();
                gpsLooper = null;
            }
            
            getServletContext().removeAttribute(LAST_TRACKER_REQUEST_TIME); //reset last request time
            providerLocations.clear(); //empty the known locations
        }
    }
    
    
    
    public void checkInterRequestGap ()
    {
        synchronized (sync)
        {
            Long lastTrackerRequestTime = (Long)getServletContext().getAttribute(LAST_TRACKER_REQUEST_TIME);
            
            if (lastTrackerRequestTime == null)
                return;
            long gap = System.currentTimeMillis() - lastTrackerRequestTime.longValue();
            if (gap > INTER_REQUEST_TIMEOUT)
            {
                Log.d(TAG, "ControlThread stopping trackers: request gap = "+gap);
                stopTrackers();
            }
            else
                Log.d(TAG, "ControlThread, gap too small: "+gap);
        }
    }


    @Override
    public void destroy()
    {
        stopTrackers();  
        controlThread.interrupt();
        super.destroy();
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    
    public Location lastLocation ()
    {
        Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        
        synchronized (sync)
        {
            Location gps = providerLocations.get(LocationManager.GPS_PROVIDER);
            Location network = providerLocations.get(LocationManager.NETWORK_PROVIDER);
            
            //if no existing gps location, or if last known location is more recent
            if (gps == null || (lastGps != null && lastGps.getTime() > gps.getTime()))
                providerLocations.put(LocationManager.GPS_PROVIDER, lastGps);
           
                
            if (network == null || (lastNetwork != null && lastNetwork.getTime() > network.getTime()))
                providerLocations.put(LocationManager.NETWORK_PROVIDER, lastNetwork);
                
           return getLocation();
        }
        
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String action = request.getParameter("action");
        if (action == null)
        {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        if (action.equalsIgnoreCase("last"))
        {
            Location l = lastLocation();
            sendLocation(response,l);
            return;
        }

        if (action.equalsIgnoreCase("update"))
        {
            Location l = null;
            synchronized (sync)
            {
                if (!tracking.get())
                {
                    //not tracking
                    sendError(response, "Not tracking");
                    return;
                }
                //update time of request
                getServletContext().setAttribute(LAST_TRACKER_REQUEST_TIME, new Long(System.currentTimeMillis()));
                l = getLocation();
            }  

            sendLocation (response, l);
            return;
        }

    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String action = request.getParameter("action");
        if (action == null)
        {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        if (action.equalsIgnoreCase("ring"))
        {
            ring(request.getParameter("sec"));
            response.setStatus(HttpServletResponse.SC_OK);
            
            return;
        } 
        
        if (action.equalsIgnoreCase("track"))
        {
         
            Location l = null;
            synchronized (sync)
            {
                //start tracking if not already started
                startTrackers(); 

                //update time of last request
                getServletContext().setAttribute(LAST_TRACKER_REQUEST_TIME, new Long(System.currentTimeMillis()));

                //if no  location, wait a while to see if the tracker has got one
                l = getLocation();
                if (l == null)
                {
                    try
                    {
                        sync.wait(CONTINUATION_TIMEOUT);
                    }
                    catch (InterruptedException e)
                    {
                        Log.e(TAG, "Interrupted waiting for Location", e);
                    } 
                    l = getLocation();
                }
            }
            
            sendLocation (response, l);
            return;
        }
        
        if (action.equalsIgnoreCase("stopTrack"))
        {
            Log.i(TAG, "Stopping trackers");
            stopTrackers(); //stop the trackers
            sendEmpty(response);
            return;
        }
    }
    
    
    
    
    public void ring (String sec) 
    {
        int seconds = DEFAULT_RING_SEC;
        if (sec != null)
        {
            sec = sec.trim();
            if (!"".equals(sec))
                seconds = Integer.valueOf(sec);
        }
        
        if (mediaPlayer == null)
        {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(androidContext, RingtoneManager.TYPE_RINGTONE);
            mediaListener = new MediaListener();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try
            {
                mediaPlayer.setDataSource(androidContext, uri);
                mediaPlayer.setOnPreparedListener(mediaListener);
                mediaPlayer.setOnCompletionListener(mediaListener);
                mediaPlayer.setOnErrorListener(mediaListener);
            }
            catch (Exception e)
            {
                Log.i(TAG, "Error preparing ring", e);
            }
        }
       
        if (!mediaPlayer.isPlaying())
        {
            mediaPlayer.prepareAsync();
        }
        else
            Log.i(TAG, "Ring already playing");
    }
    
    
    public Location getLocation ()
    {
        //Go for finest grained location via GPS first
        Location l;
        synchronized (sync)
        {
            Location gps = providerLocations.get(LocationManager.GPS_PROVIDER);
            Location network = providerLocations.get(LocationManager.NETWORK_PROVIDER);

            if (gps != null)
            {
                if (network == null)
                {
                    l = gps;
                }
                else
                {
                    if (network.getTime() > gps.getTime())
                        l = network; //most recent
                    else
                        l = gps;
                }
            }
            else
                l = network;
        }
        return l;
    }
    
    
    
    
    public void sendLocation (HttpServletResponse response, Location location) throws IOException
    {
        response.setContentType("text/json");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        StringBuffer buff = new StringBuffer();

        asJson(buff, location);

        writer.println(buff.toString());
    }
    
    
    public void sendError (HttpServletResponse response, String error)  throws IOException
    {
        response.setContentType("text/json");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        StringBuffer buff = new StringBuffer();
        jsonError(buff, error);
        writer.println(buff.toString());
    }
    
    public void sendEmpty (HttpServletResponse response)  throws IOException
    {
        response.setContentType("text/json");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        writer.println("{}");
    }
    
    private void asJson (StringBuffer buff, Location location)
    {
        if (buff == null)
            return;
        
        if (location == null)
            jsonError(buff, "No location");
        else
        {
            buff.append("{ \"location\": { ");
            buff.append("                  \"lat\": "+location.getLatitude()+",");
            buff.append("                  \"long\": "+location.getLongitude()+",");
            buff.append("                  \"time\": "+location.getTime());
            buff.append("                }");
            buff.append("}");
        }
    }
    
    private void jsonError(StringBuffer buff, String err)
    {
        if (buff == null)
            return;
        
        buff.setLength(0);
        buff.append("{ \"error\": \""+err+"\"}");
    }
    
}
