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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.ijetty.console.HTMLHelper;
import org.mortbay.util.IO;
import org.mortbay.util.URIUtil;

import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.net.Uri;
import android.database.Cursor;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
    
    private int __THUMB_WIDTH = 120;
    private int __THUMB_HEIGHT = 120;
    
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
        Log.w("Jetty", "Running doGet in MediaBrowserServlet.java");
        
        String pathInfo = request.getPathInfo();
        String servletPath=request.getServletPath();
        String pathInContext=URIUtil.addPaths(servletPath,pathInfo);
        
        if (pathInfo == null) {
            Log.w("Jetty", "pathInfo was null, returning 404");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String what = null; // 'json' or 'fetch'
        String typestr = null; // media type id (0, 1) if what='fetch', media type string (audio, images, etc) if what='json'
        String item = null; // item id
        String thumb = null; // 'thumb' if only if thumbnail requested
        
        StringTokenizer strtok = new StringTokenizer(pathInfo, "/");
        if (strtok.hasMoreElements())
            what = strtok.nextToken();
        
        if (strtok.hasMoreElements())
            typestr = strtok.nextToken();
        
        if (strtok.hasMoreElements())
            item = strtok.nextToken();
        
        if (strtok.hasMoreElements())
            thumb = strtok.nextToken();
        
        if ("fetch".equals(what.trim())) {
            try {
                Long rowid = Long.parseLong (item);
                int type = Integer.parseInt (typestr);
                
                Uri contenturi = __MEDIA_URIS[type];
                Uri content = Uri.withAppendedPath(contenturi, item);
                
                if (thumb != null && "thumb".equals(thumb.trim()))
                {
                    Bitmap bitmap_orig = MediaStore.Images.Media.getBitmap(resolver, content);
                    if (bitmap_orig != null)
                    {
                        response.setStatus(HttpServletResponse.SC_OK);
                        InputStream stream = null;
                        OutputStream os = response.getOutputStream();
                        Bitmap bitmap = null;

                        int width = bitmap_orig.getWidth();
                        int height = bitmap_orig.getHeight();
                        
                        // If the image is too big (AND the width/height isn't 0), scale it
                        if ((width > __THUMB_WIDTH || height > __THUMB_HEIGHT) && height != 0 && width != 0)
                        {
                            float scaleWidth = 0;
                            float scaleHeight = 0;
                            
                            if (width > __THUMB_WIDTH)
                                scaleWidth = ((float) __THUMB_WIDTH) / width;
                            
                            if (height > __THUMB_HEIGHT)
                                scaleHeight = ((float) __THUMB_HEIGHT) / height;
                            
                            if (scaleHeight < scaleWidth) 
                                scaleWidth = scaleHeight;
                            else if (scaleWidth < scaleHeight)
                                scaleHeight = scaleWidth;
                            
                            if (scaleWidth == 0)
                                scaleWidth = 0.5f;
                            
                            if (scaleHeight == 0)
                                scaleHeight = 0.5f;

                            Matrix matrix = new Matrix();
                            matrix.postScale(scaleWidth, scaleHeight);

                            // recreate the new Bitmap
                            bitmap = Bitmap.createBitmap(bitmap_orig, 0, 0, width, height, matrix, true);
                            response.setContentType("image/png");
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                            stream = new ByteArrayInputStream(bytes.toByteArray());
                        }
                        else
                        {
                            // just return the original data from the DB
                            response.setContentType(resolver.getType(content));
                            stream = resolver.openInputStream(content);
                        }

                        IO.copy(stream,os);
                    }
                }
                else
                {
                    Log.i("Jetty", "thumb = " + thumb);
                    response.setContentType(resolver.getType(content));
                    InputStream stream = resolver.openInputStream(content);
                    OutputStream os = response.getOutputStream();
                    
                    response.setStatus(HttpServletResponse.SC_OK);
                    IO.copy(stream, os);
                }
            } catch (Exception e) {
                Log.w("Jetty", "Failed to fetch media", e);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        else if ("json".equals(what.trim()))
        {
            response.setContentType("text/json; charset=utf-8");
            PrintWriter writer = response.getWriter();
            
            Integer type = -1;
            
            if ("images".equals(typestr.trim())) {
                type = 0;
            } else if ("audio".equals(typestr.trim())) {
                type = 2;
            } else if ("video".equals(typestr.trim())) {
                type = 4;
            }
            
            if (type == -1) {
                Log.w("Jetty", "Invalid media type " + typestr);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            writer.print("[ ");
            
            Integer limit = type + 1;
            while (type <= limit)
            {
                Uri contenturi = __MEDIA_URIS[type];
                Cursor cur = resolver.query (contenturi, null, null, null, null);
                
                while (cur.moveToNext())
                {
                    Long rowid = cur.getLong (cur.getColumnIndexOrThrow(BaseColumns._ID));
                    String name = cur.getString (cur.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE));
                    
                    writer.print (" { 'type' : " + type.toString() + ", 'id' : " + rowid.toString() + ", 'title' : '" + name.replace("'", "\\'") + "', ");
                    
                    if (contenturi == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI || contenturi == MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                    {
                        int music = cur.getInt (cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_MUSIC));
                        if (music != 0)
                        {
                            String artist = cur.getString (cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
                            String album = cur.getString (cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
                            
                            writer.print ("'artist' : '" + artist + "', 'album' : '" + album + "' ");
                        }
                    }
                    
                    writer.print ("}");
                    
                    if (!cur.isLast())
                        writer.print (",");
                }
                
                type++;
            }
            
            writer.print(" ]");
        }
        else
        {
            Log.w("Jetty", "invalid action - '" + what + "', returning 404");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
