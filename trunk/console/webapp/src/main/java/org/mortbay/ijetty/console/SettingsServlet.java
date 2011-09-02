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
    private ContentResolver resolver;
    android.content.Context androidContext;
    

    protected void doJSON (PrintWriter writer, HttpServletRequest request,
            HttpServletResponse response) 
    {
        Cursor cursor = null;
        try
        {
            writer.println("{ \"settings\": {");
            writer.println("    \"headings\": [");
            cursor = getContentResolver().query(Settings.System.CONTENT_URI, null, null, null, Settings.System.NAME+" ASC");
            if (cursor != null && cursor.getColumnNames() != null)
            {
                for (int i=0;i<cursor.getColumnNames().length; i++)
                {
                    writer.print("\""+cursor.getColumnNames()[i]+"\"");
                    if (i<cursor.getColumnNames().length -1)
                        writer.println(",");
                }
            }
            writer.println("              ],");
            writer.println("    \"rows\": [");
            while (cursor != null && cursor.moveToNext())
            {
                writer.println("        [");
                for (int i = 0; i < cursor.getColumnNames().length; i++)
                {
                    String val = cursor.getString(i);
                    writer.print("\""+val+"\"");
                    if (i < cursor.getColumnNames().length-1)
                        writer.println(",");
                }
                writer.println("        ]");
                if (!cursor.isLast())
                    writer.println(",");
            }
            writer.println("          ]");
            
            writer.println("         }");
            writer.println("}");
        }
        finally
        {
            if (cursor != null)
                cursor.close();
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/json");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        doJSON(writer, request, response);
       
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
        androidContext = (android.content.Context)config.getServletContext().getAttribute("org.mortbay.ijetty.context");
    }

}
