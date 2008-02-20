package org.mortbay.ijetty;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

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

public class IJettyService extends Service 
{

    private NotificationManager mNM;
    private Server server;
    

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
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // This is who should be launched if the user selects our persistent
        // notification.
        Intent intent = new Intent(this, IJetty.class);

        // Display a notification about us starting.
        mNM.notify(R.string.jetty_started,
                   new Notification(
                       R.drawable.jicon,
                       getText(R.string.manage_jetty),
                       intent,
                       getText(R.string.jetty_started),
                       null));
        
        
        server = new Server();
        Connector connector=new SocketConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});
        
        Handler handler=new HelloHandler();
        server.setHandler(handler);
        
        try
        {
            System.setProperty("org.mortbay.log.class","org.mortbay.log.AndroidLog");
            Log.i("Jetty", "Jetty starting");
            org.mortbay.log.Log.setLog(new AndroidLog());
            server.start();
            server.join();
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error starting jetty", e);
        }
    }

    @Override
    protected void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.jetty_started);

        // Tell the user we stopped.
        mNM.notifyWithText(R.string.jetty_stopped,
                   getText(R.string.jetty_stopped),
                   NotificationManager.LENGTH_SHORT,
                   null);

        try
        {
            server.stop();
            server=null;
            Log.i("Jetty", "Jetty stopped");
        }
        catch (Exception e)
        {
            Log.e("Jetty", "Error stopping jetty", e);
        }  
    }

	/** 
	 * Do not allow clients to bind to the jetty service for now.
	 * TODO investigate binding
	 * @see android.app.Service#getBinder()
	 */
	@Override
	public IBinder getBinder() 
	{
		return null; 
	}
	
}
