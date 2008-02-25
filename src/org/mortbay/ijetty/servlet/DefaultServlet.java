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
import org.mortbay.resource.ResourceFactory;
import org.mortbay.util.IO;

import android.util.Log;
import android.content.Resources;


public abstract class DefaultServlet extends HttpServlet
{

    private Resources _resources;
    
    public void setResources (Resources resources)
    {
        _resources=resources;
    }
    @Override
    protected abstract void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException;

    @Override
    protected abstract void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException;
    
 
    
    
 

}
