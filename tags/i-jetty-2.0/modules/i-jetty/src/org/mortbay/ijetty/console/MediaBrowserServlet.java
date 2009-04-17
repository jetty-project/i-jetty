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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.console.HTMLHelper;
import org.mortbay.util.IO;

import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.net.Uri;
import android.database.Cursor;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.util.Log;

public class MediaBrowserServlet extends HttpServlet
{
    private Uri[] __MEDIA_URIS = {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Images.Media.INTERNAL_CONTENT_URI,
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        MediaStore.Video.Media.INTERNAL_CONTENT_URI
    };
    
    private String[] __MEDIA_LABELS = { "Images", "Audio", "Video" };
    
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
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            // If nothing was passed (including ending /)
            // just do default stuff
            doBody(request, response);
            return;
        }
        
        String typestr = null;
        String item = null;
        
        StringTokenizer strtok = new StringTokenizer(pathInfo, "/");
        if (strtok.hasMoreElements())          
            typestr = strtok.nextToken();
        
        if (strtok.hasMoreElements())          
            item = strtok.nextToken();
        
        if (typestr == null || item == null) {
            // Either type and item weren't found (both required)
            doBody(request, response);
            return;
        }
        
        try {
            Long rowid = Long.parseLong (item);
            int type = Integer.parseInt (typestr);
            
            Uri contenturi = __MEDIA_URIS[type];
            Uri content = Uri.withAppendedPath(contenturi, item);
            
            response.setContentType(resolver.getType(content));
            response.setStatus(HttpServletResponse.SC_OK);
            InputStream stream = resolver.openInputStream(content);
            OutputStream os = response.getOutputStream();
            IO.copy(stream, os);
            
        } catch (Exception e) {
            Log.w("Jetty", "Failed to fetch media", e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
    }

    protected void doBody(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        HTMLHelper.doHeader(writer, request, response);
        HTMLHelper.doMenuBar(writer, request, response);
        
        writer.println("<h1 class='pageheader'>Media</h1><div id='content'>");
        
        Integer type = 0;
        int count = 0;
        boolean new_list = true;
        
        for (Uri contenturi : __MEDIA_URIS)
        {
            if (type % 2 == 0)
            {
                writer.println("<h2>" + __MEDIA_LABELS[type / 2] + "</h2>");
                writer.println("<ul>");
            }
            
            Cursor cur = resolver.query (contenturi, null, null, null, null);
            while (cur.moveToNext())
            {
                Long rowid = cur.getLong (cur.getColumnIndexOrThrow(BaseColumns._ID));
                String name = cur.getString (cur.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                
                writer.println("<li><a href='/console/media/" + type.toString() + "/" + rowid.toString() + "'>" + name + "</a></li>");
                count++;
            }
            
            type++;
            
            if (type % 2 == 0)
            {
                if (count == 0)
                    writer.println("<li>No media items found on this phone.</li>");
                count = 0;
            
                writer.println("</ul>");
            }
        }
        
        writer.println("</div>");
        
        HTMLHelper.doFooter (writer, request, response);
    }
}
