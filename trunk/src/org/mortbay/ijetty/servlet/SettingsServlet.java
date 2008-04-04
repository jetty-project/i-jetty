package org.mortbay.ijetty.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.database.Cursor;
import android.provider.Settings;

public class SettingsServlet extends InfoServlet
{

    @Override
    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
        
        writer.println("<h1>System Settings</h1>");
        Cursor cursor = getContentResolver().query(Settings.System.CONTENT_URI, null, null, null, null);
        String[] cols = cursor.getColumnNames();
        formatTable(cols, cursor, writer);
    }

}
