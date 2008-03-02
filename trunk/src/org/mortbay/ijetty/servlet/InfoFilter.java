package org.mortbay.ijetty.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.URIUtil;

public class InfoFilter implements Filter
{

    public void destroy()
    {
        // TODO Auto-generated method stub
        
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String servletPath=httpRequest.getServletPath();
        String pathInfo=httpRequest.getPathInfo();
        String pathInContext=URIUtil.addPaths(servletPath,pathInfo);
        boolean endsWithSlash=pathInContext.endsWith(URIUtil.SLASH);
           
        
        if (pathInContext.equals("")||pathInContext.equals("/"))
        {
            if (!endsWithSlash)
                pathInContext+="/";
            
            RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(pathInContext+"app");
            dispatcher.forward(request, response);
        
        }
        else
            chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException
    {   
        
    }

}
