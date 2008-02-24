package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.R;
import org.mortbay.util.IO;

import android.util.Log;
import android.content.Resources;


public class StaticContentServlet extends HttpServlet
{

    private Resources _resources;
    
    public void setResources (Resources resources)
    {
        _resources=resources;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        serveFile (req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        serveFile (req, resp);
    }
    private void serveFile (HttpServletRequest req, HttpServletResponse resp) 
    throws ServletException, IOException
    {
        String path = req.getPathInfo();
        if (path != null)
        {
            int x = path.lastIndexOf("/");
            if (path.length()>x+1)
                path = path.substring(x+1);
            
            path = path.replace(".", "_");
            Log.i("Jetty", "Static content path="+path);
            int id = -1;
            //this is the name of a file - try to get it from the classpath to serve
            Class[] classes = R.class.getClasses();
            if (classes!=null)
            {
                Class rawClass = null;
                for (int i=0;i<classes.length;i++)
                {
                    if (classes[i].getName().equals("raw"))
                        rawClass=classes[i];
                }


                if (rawClass != null)
                {
                    Field idField = null;
                    try
                    {
                        idField = rawClass.getDeclaredField(path);
                        id = idField.getInt(null);
                    }
                    catch (NoSuchFieldException e)
                    {
                    }
                    catch (IllegalAccessException e)
                    {
                        
                    }
                }
            }

            if (id >= 0)
            {
                InputStream is = _resources.openRawResource(id);
                if (is!=null)
                {
                    OutputStream os = resp.getOutputStream();
                    IO.copy(is, os);
                }
                else
                    resp.sendError(resp.SC_NOT_FOUND);
            }
        }
        else
            resp.sendError(resp.SC_NOT_FOUND); 
    }

}
