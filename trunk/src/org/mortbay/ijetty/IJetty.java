package org.mortbay.ijetty;

import java.io.IOException;

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

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class IJetty extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        TextView tv = new TextView(this);
        
        Server server = new Server();
        Connector connector=new SocketConnector();
        connector.setPort(8080);
        server.setConnectors(new Connector[]{connector});
        
        Handler handler=new HelloHandler();
        server.setHandler(handler);

        
        String msg="Hello";
        try
        {
            server.start();
            tv.setText(msg+" from I-Jetty: "+connector.getConnection());
        }
        catch (Exception e)
        {
            msg=e.toString();
            e.printStackTrace();
            tv.setText(msg+" from I-Jetty! ");
        }

        setContentView(tv);
        
    }
    

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

}