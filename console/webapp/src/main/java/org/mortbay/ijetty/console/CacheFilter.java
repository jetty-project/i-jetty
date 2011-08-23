package org.mortbay.ijetty.console;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class CacheFilter implements Filter
{

    public void init(FilterConfig filterConfig) throws ServletException
    {
        // TODO Auto-generated method stub

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        try
        {
            HttpServletRequest httpRequest = (HttpServletRequest)request;
            
            //Do not cache: any restful request
            
            
            String path = httpRequest.getServletPath();
            System.err.println(path);
            if (path.contains("rest"))
            {
                ((HttpServletResponse)response).setHeader("Cache-Control", "no-store");
            }
            
        }
        finally
        {
            chain.doFilter(request, response);
        }
    }

    public void destroy()
    {
        // TODO Auto-generated method stub

    }

}
