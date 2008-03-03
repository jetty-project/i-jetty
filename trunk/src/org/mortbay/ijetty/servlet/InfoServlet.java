package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.Contacts;
import android.provider.Settings;

public abstract class InfoServlet extends HttpServlet
{
    private ContentResolver _resolver;

    public void setContentResolver (ContentResolver resolver)
    {
        _resolver=resolver;
    }
    public ContentResolver getContentResolver ()
    {
        return _resolver;
    }
    protected void doHeader (PrintWriter writer,HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<html>");
        writer.println("<head><META http-equiv=\"Pragma\" content=\"no-cache\"> <META http-equiv=\"Cache-Control\" content=\"no-cache,no-store\">");
        writer.println(" <link rel=\"stylesheet\" type=\"text/css\" href=\"/app/css\" /></head>");
    }
    
    protected void doFooter (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("</html>");
    }
    
    protected abstract void doContent (PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        base_request.setHandled(true);

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        doHeader(writer, request, response);
        doContent(writer, request, response);
        doFooter (writer, request, response);
    }

 
    protected void formatTable (String[] colNames, Cursor cursor, PrintWriter writer)
    {   
        if (colNames!=null && cursor!=null && writer!=null)
        {
            writer.println("<table>");
            writer.println("<tr>");
            for (int i=0;i<colNames.length;i++)
                writer.println("<th>"+colNames[i]+"</th>");
            writer.println("</tr>");
            int row = 0;
            while (cursor.next())
            {  
                String style = "";
                if (row%2==0)
                    style = "even";
                else
                    style = "odd";

                writer.println("<tr>");
                for (int i=0;i<colNames.length;i++)
                {
                    String val=cursor.getString(i);
                    writer.println("<td class=\""+style+"\">"+(val==null?"&nbsp;":val)+"</td>");
                }
                writer.println("</tr>");
                ++row;
            }
            writer.println("</table>");

        }
    }
    
    protected String getRowStyle (int row)
    {
        if (row%2==0)
            return "even";
        else
            return "odd";

    }
}
