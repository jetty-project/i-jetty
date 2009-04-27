package org.mortbay.ijetty.hello;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/* ------------------------------------------------------------ */
/** Hello Servlet 
 * 
 */
public class HelloWorld extends HttpServlet 
{
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config) throws ServletException
    {
    	super.init(config);
    	//to demonstrate it is possible
        Object o = config.getServletContext().getAttribute("contentResolver");
        android.content.ContentResolver resolver = (android.content.ContentResolver)o;
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        ServletOutputStream out = response.getOutputStream();
        out.println("<html>");
        out.println("<h1>Hello From Servlet Land!</h1>");
        out.println("</html>");
        out.flush();
    }
    
}
