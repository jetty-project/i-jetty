package org.mortbay.ijetty;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.Contacts;
import android.provider.CallLog;
import android.provider.Settings;
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
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;

public class IJettyService extends Service
{

    private NotificationManager mNM;
    private Server server;
    private static Resources __resources;
    private SharedPreferences preferences;

    public class HelloHandler extends AbstractHandler
    {
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
            base_request.setHandled(true);
            
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter writer = response.getWriter();
            ContentResolver resolver = getContentResolver();
            Map params = request.getParameterMap();
            boolean wantContacts = params.get("contacts")!=null;
            boolean wantSystemSettings = params.get("system")!=null;
            boolean wantCallLog = params.get("calls")!=null;
            
            if (params.isEmpty())
            {
                writer.println("<h1>Options</h1>");
                writer.println("<ul>");
                writer.println("<li><a href='/?contacts'>Contacts</a></li>");
                writer.println("<li><a href='/?system'>System Settings</a></li>");
                writer.println("<li><a href='/?calls'>Call Log</a></li>");
                writer.println("</ul>");
            }
            else if (wantContacts)
              doContacts(resolver, writer);
            else if (wantSystemSettings)
                doSystemSettings (resolver, writer);
            else if (wantCallLog)
                doCallLog (resolver, writer);
        }
        
        public void doContacts (ContentResolver resolver, PrintWriter writer)
        {
            String[] projection = new String[] {
                    android.provider.BaseColumns._ID,
                    android.provider.Contacts.PeopleColumns.NAME,
                    android.provider.Contacts.PhonesColumns.NUMBER,
                    android.provider.Contacts.PeopleColumns.PHOTO};
            Cursor cursor = resolver.query(Contacts.People.CONTENT_URI, projection, null, null, null);  
            if (cursor!=null)
            {
                String[] cols = cursor.getColumnNames();
                formatTable (cols, cursor, writer);
            }
        }

        public void doCallLog (ContentResolver resolver, PrintWriter writer)
        {
            String[] projection = new String[] 
                                             {
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.NEW,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.NUMBER_TYPE,
                    CallLog.Calls.NAME
                                             };
            Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, null);
            writer.println("<h1>Call Log</h1>");
            String[] cols = cursor.getColumnNames();
            formatTable(cols, cursor, writer);  
        }

        public void doSystemSettings (ContentResolver resolver, PrintWriter writer)
        {  
            String[] projection = new String[] 
                                             {
                    Settings.System._ID,
                                             };
            writer.println("<h1>System Settings</h1>");
            Cursor cursor = resolver.query(Settings.System.CONTENT_URI, null, null, null, null);
            String[] cols = cursor.getColumnNames();
            formatTable(cols, cursor, writer);
        }
        
        private void formatTable (String[] colNames, Cursor cursor, PrintWriter writer)
        {   
            if (colNames!=null && cursor!=null && writer!=null)
            {
                writer.println("<table style='border-width:3px;border-style:groove;border-color:#aaaaaa;'>");
                writer.println("<tr>");
                for (int i=0;i<colNames.length;i++)
                    writer.println("<th style='font-variant:small-caps;align:left;'>"+colNames[i]+"</th>");
                writer.println("</tr>");
                int row = 0;
                while (cursor.next())
                {  
                    String style = "";
                    if (row%2==0)
                        style = "'background:#ffffff;border:solid #aaaaaa 1px;'";
                    else
                        style = "'background:#efefef;border:solid #aaaaaa 1px;'";
                    
                    writer.println("<tr>");
                    for (int i=0;i<colNames.length;i++)
                            writer.println("<td style="+style+">"+cursor.getString(i)+"</td>");

                    writer.println("</tr>");
                    ++row;
                }
                writer.println("</table>");

            }
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
        //Connector connector=new SocketConnector();
        Connector connector=new SelectChannelConnector();
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
            preferences = getSharedPreferences("jetty", MODE_WORLD_READABLE);
            Editor editor = preferences.edit();
            editor.putBoolean("isRunning", true);
            editor.commit();
            
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
                server.join();
                server=null;

                Editor editor = preferences.edit();
                editor.putBoolean("isRunning", false);
                editor.commit();

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
