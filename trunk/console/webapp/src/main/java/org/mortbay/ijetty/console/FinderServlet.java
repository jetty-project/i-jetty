package org.mortbay.ijetty.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import android.content.ContentResolver;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class FinderServlet extends HttpServlet
{
    private static final String TAG = "IJetty.Cnsl";
    public static final String LAST_NETWORK_LOCATION = "org.mortbay.ijetty.console.lastNetworkLocation";
    public static final String LAST_GPS_LOCATION = "org.mortbay.ijetty.console.lastGPSLocation";
    public static final String NETWORK_LISTENER = "org.mortbay.ijetty.console.networkListener";
    public static final String GPS_LISTENER = "org.mortbay.ijetty.console.gpsListener";
    public static final String LOCATION_MANAGER = "org.mortbay.ijetty.console.locationManager";
    public static final long TIMEOUT = 30000; //wait up to 30sec
    android.content.Context androidContext;
    ContentResolver resolver;
    LocationManager locationManager;
    public Map<String,Location> providerLocations = Collections.synchronizedMap(new HashMap<String,Location>());
    Thread gpsLooper;
    Thread networkLooper;
    AtomicInteger trackers = new AtomicInteger();
    

    class LooperThread extends Thread 
    {
        String provider;
        Looper looper;
        
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
            LocationManager manager = (LocationManager)androidContext.getSystemService(Context.LOCATION_SERVICE);
            AsyncLocationListener listener = new AsyncLocationListener(provider);
            manager.requestLocationUpdates(provider, 60000L, 0F, listener, Looper.getMainLooper()); //Get an update every 60 secs
            Log.d(TAG, "Requested location updates for "+provider);
            Looper.loop();
        }
      
        
        public void quit ()
        {
           looper.quit();
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
            Log.d(TAG, "location change: "+location);
            providerLocations.put(provider, location);
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
    }


    public void startTrackers()
    {
        synchronized (providerLocations)
        {
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
        synchronized (providerLocations)
        { 
            if (networkLooper != null)
            {
                ((LooperThread)networkLooper).quit();
                networkLooper.interrupt();
                networkLooper = null;
            }

            if (gpsLooper != null)
            {
                ((LooperThread)gpsLooper).quit();
                gpsLooper.interrupt();
                gpsLooper = null;
            }
        }
    }


    @Override
    public void destroy()
    {
        stopTrackers();        
        super.destroy();
    }


    
    public Location lastLocation ()
    {
        Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        
        synchronized (providerLocations)
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
            Location l = getLocation();
            sendLocation(response,l);
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
            ring();
            response.setStatus(HttpServletResponse.SC_OK);
            
            return;
        } 
        
        if (action.equalsIgnoreCase("track"))
        {
            Log.d(TAG, "FinderServlet");
            
            if (trackers.incrementAndGet() == 1)
            {
                startTrackers();
            }
            
            final Continuation continuation = ContinuationSupport.getContinuation(request);
            Location l = getLocation();
            if (l == null)
            {
                Log.d(TAG, "No location");
                //no location, we need to get a continuation if we can. If we already
                //did do a continuation and we've come back, we just have to wear the
                //null location

                if (continuation.isInitial())
                {
                    continuation.setTimeout(TIMEOUT);
                    continuation.suspend();  
                    Log.d(TAG, "Suspending");
                    return;
                }
            }
            
            //We are expired or resumed, did we find a location?
            sendLocation (response, l);

            return;
        }
        
        if (action.equalsIgnoreCase("stopTrack"))
        {
            //decrease the count of users tracking the device, when its 0, stop the loop
            if (trackers.decrementAndGet() <= 0)
            {
                Log.d(TAG, "Stopping trackers");
                stopTrackers();
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/json");
            PrintWriter writer = response.getWriter();
            StringBuffer buff = new StringBuffer();
            buff.append("{}");
            writer.println(buff.toString());
            return;
        }
    }
    
    
    public void ring () 
    {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(androidContext, RingtoneManager.TYPE_RINGTONE);
        System.err.println("Uri="+uri);
        Ringtone tone = RingtoneManager.getRingtone(androidContext, uri);
        if (!tone.isPlaying())
            tone.play();
    }
    
    
    public Location getLocation ()
    {
        //Go for finest grained location via GPS first
        Location l;
        synchronized (providerLocations)
        {
            Location gps = providerLocations.get(LocationManager.GPS_PROVIDER);
            Location network = providerLocations.get(LocationManager.NETWORK_PROVIDER);

            if (gps != null)
            {
                if (network == null)
                    l = gps;
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

        asJson(buff, getLocation());

        writer.println(buff.toString());
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
