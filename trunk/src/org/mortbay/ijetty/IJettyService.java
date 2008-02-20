package org.mortbay.ijetty;

import java.io.IOException;
import java.io.InputStream;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;

public class IJettyService extends Service
{

    private NotificationManager mNM;
    private Server server;
    private static Resources __resources;
    

    public static class HelloHandler extends AbstractHandler
    {
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
            base_request.setHandled(true);
            
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>Hello OneHandler</h1>");
        }
    }

  
    
    
    @Override
    protected void onCreate() 
    {
        __resources = getResources();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // This is who should be launched if the user selects our persistent
        // notification.
        Intent intent = new Intent(this, IJetty.class);

        Toast.makeText(IJettyService.this, R.string.jetty_started, Toast.LENGTH_SHORT).show();
        mNM.notify(R.string.jetty_started,
                   new Notification(
                       this,
                       R.drawable.jicon,
                       getText(R.string.manage_jetty),
                       System.currentTimeMillis(),
                       getText(R.string.manage_jetty),
                       getText(R.string.manage_jetty),
                       intent,
                       R.drawable.jicon,
                       getText(R.string.manage_jetty),
                       intent));
        server = new Server();
        Connector connector=new SocketConnector();
        //Connector connector=new SelectChannelConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});
        Handler handler=new HelloHandler();
        server.setHandler(handler);
        try
        {
            // TODO Auto-generated method stub
            System.setProperty("org.mortbay.log.class","org.mortbay.log.AndroidLog");
            Log.i("Jetty", "Jetty starting");
            org.mortbay.log.Log.setLog(new AndroidLog());
            server.start();
            Log.i("Jetty", "Jetty started");
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error starting jetty", e);
        }
    }

    @Override
    protected void onDestroy() {

        try
        {
            if (server != null)
            {
                // Cancel the persistent notification.
                mNM.cancel(R.string.jetty_started);

                // Tell the user we stopped.
                Toast.makeText(this, getText(R.string.jetty_stopped), Toast.LENGTH_SHORT).show();
             

                Log.i("Jetty", "Jetty stopping");
                server.stop();
                Log.i("Jetty", "About to do join");
                server.join();
                server=null;
                Log.i("Jetty", "****Jetty stopped");
                __resources=null;
            }
            else
                Log.i("Jetty", "Jetty not running");

        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error stopping jetty", e);
        }  
    }


	
	
	/**
	 * Hack to get around bug in ResourceBundles
	 * @param id
	 * @return
	 */
	public static InputStream getStreamToRawResource (int id)
	{
	    if (__resources != null)
	        return __resources.openRawResource(id);  
	    else
	        return null;
	}

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
