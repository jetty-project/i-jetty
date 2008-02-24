package org.mortbay.ijetty.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StreamServlet extends HttpServlet
{
   
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException
        {
            serveStream (req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException
        {
            serveStream(req, resp);
        }
        
        public void serveStream (HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
        {
            
        }
    
}
