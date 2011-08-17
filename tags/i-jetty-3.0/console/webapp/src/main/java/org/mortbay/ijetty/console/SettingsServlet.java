//========================================================================
//$Id$
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.ijetty.console;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Settings;

public class SettingsServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final String __HUMAN_ID = "ID";
    private ContentResolver resolver;


    protected void doContent(PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {

        writer.println("<h1 class='pageheader'>System Settings</h1><div id='content'>");
        Cursor cursor = getContentResolver().query(Settings.System.CONTENT_URI, null, null, null, null);
        String[] cols = cursor.getColumnNames();
        int i = 0;

        for (String col : cols)
        {
            if (i == 0)
            {
                cols[i] = __HUMAN_ID;
            }
            else
            {
                // Make first letter uppercase
                cols[i] = col.substring(0, 1).toUpperCase() + col.substring(1);
            }

            i++;
        }

        HTMLHelper.formatTable(cols, cursor, writer);
        writer.println("</div>");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        HTMLHelper.doHeader(writer, request, response);
        HTMLHelper.doMenuBar(writer, request, response);
        doContent(writer, request, response);
        HTMLHelper.doFooter (writer, request, response);
    }


    public ContentResolver getContentResolver()
    {
        return resolver;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
    }

}
