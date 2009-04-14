//========================================================================
//$Id$
//Copyright 2009 Mort Bay Consulting Pty. Ltd.
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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.console.HTMLHelper;

import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.net.Uri;
import android.database.Cursor;
import android.content.ContentResolver;

public class MediaBrowserServlet extends HttpServlet
{
    private Uri[] __MEDIA_URIS = { MediaStore.Images.Media.EXTERNAL_CONTENT_URI };
    
    private ContentResolver resolver;

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("contentResolver");
    }

    public ContentResolver getContentResolver()
    {
        return resolver;
    }

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

    protected void doContent(PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        writer.println("<h1 class='pageheader'>Media</h1><div id='content'>");
        
        List <Uri> media = new ArrayList <Uri> ();
        
        writer.println("<ul>");
        
        int count = 0;
        
        for (Uri contenturi : __MEDIA_URIS)
        {
            Cursor cur = resolver.query (contenturi, null, null, null, null);
            cur.moveToFirst();
            while (cur.moveToNext())
            {
                Long rowid = cur.getLong (cur.getColumnIndexOrThrow(BaseColumns._ID));
                String name = cur.getString (cur.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                
                media.add (Uri.withAppendedPath(contenturi, rowid.toString()));
                writer.println("<li>" + name + "</li>");
                count++;
            }
        }
        
        if (count == 0)
            writer.println("<li>No media items found on this phone.</li>");
        
        writer.println("</ul>");
        writer.println("</div>");
    }

}
