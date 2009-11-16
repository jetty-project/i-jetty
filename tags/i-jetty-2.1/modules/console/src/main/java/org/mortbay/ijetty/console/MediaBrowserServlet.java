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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

public class MediaBrowserServlet extends HttpServlet
{
    public class MyMediaConnectorClient implements MediaScannerConnectionClient
    {
        private MediaScannerConnection _scanner = null;
        private final File _file;
        
        public MyMediaConnectorClient (File file)
        {
            _file = file;
        }
        
        public void setScanner (MediaScannerConnection scanner)
        {
            _scanner = scanner;
        }
        
        public void onMediaScannerConnected() {
		    _scanner.scanFile(_file.getAbsolutePath(), null);
		}

		public void onScanCompleted(String path, Uri uri) {
		    Log.i ("Jetty", "Finished scanning!");
			_scanner.disconnect();
		}
    }
    
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
    private Context context;

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
        context = (Context)getServletContext().getAttribute("org.mortbay.ijetty.context");
    }

    public ContentResolver getContentResolver()
    {
        return resolver;
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();
        
        // Should we use just the servlet directory instead?
        File sdcarddir = new File("/sdcard/jetty/media");
        
        // Create file upload directory if it doesn't exist
        if (!sdcarddir.exists())
            sdcarddir.mkdir();
        
        File output = null;
        
        try
        {
            // Save file to /sdcard
            File file = (File)request.getAttribute("fileupload");
            String origName = request.getParameter("fileupload");
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IO.copy(new FileInputStream(file), out);
            
            output = new File (sdcarddir, origName);
            Log.i ("Jetty", "Writing to: " + output);
            FileOutputStream stream = new FileOutputStream (output);
            
            out.writeTo (stream);
            stream.close();
        }
        catch (Exception e)
        {
            Log.w ("Jetty", "Failed to save uploaded file", e);
            printResponse(writer, 1, "Could not save uploaded file to sdcard.", -1);
            return;
        }
        
        // Re-run media scanner, to re-detect media        
        MyMediaConnectorClient client = new MyMediaConnectorClient(output);
		MediaScannerConnection scanner = new MediaScannerConnection(context, client);
		client.setScanner (scanner);
		
		scanner.connect();
        
        int filetype = -1;
        printResponse(writer, 0, "No error", filetype);
        
    }
    
    private void printResponse (PrintWriter writer, int resp, String msg, int filetype)
    {
        writer.println ("<script>");
        writer.println ("var json = { error: " + resp + ", msg: '" + msg + "', filetype: " + filetype + " };");
        writer.println ("if (top.Media) { top.Media.uploadComplete(json); }");
        writer.println ("</script>");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
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
        
        if (what == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
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
                            
                            Log.i("Jetty", "orig height = " + height + ", orig width = " + width);
                            Log.i("Jetty", "__THUMB_HEIGHT = " + __THUMB_HEIGHT + ", __THUMB_WIDTH = " + __THUMB_WIDTH);
                            
                            if (width > __THUMB_WIDTH)
                                scaleWidth = ((float) __THUMB_WIDTH) / width;
                            
                            if (height > __THUMB_HEIGHT)
                                scaleHeight = ((float) __THUMB_HEIGHT) / height;
                            
                            Log.i("Jetty", "scaleHeight = " + scaleHeight + ", scaleWidth = " + scaleWidth);
                            
                            if (scaleHeight < scaleWidth && scaleHeight != 0) 
                                scaleWidth = scaleHeight;
                            else if (scaleWidth < scaleHeight && scaleWidth != 0)
                                scaleHeight = scaleWidth;
                            
                            if (scaleWidth == 0)
                                scaleWidth = scaleHeight;
                            
                            if (scaleHeight == 0)
                                scaleHeight = scaleWidth;
                            
                            Log.i("Jetty", "scaleHeight = " + scaleHeight + ", scaleWidth = " + scaleWidth + " (final)");
                            
                            if (scaleHeight == 0)
                            {
                                Log.w("Jetty", "scaleHeight and scaleWidth both = 0! Setting scale to 50%.");
                                scaleHeight = 0.5f;
                                scaleWidth = 0.5f;
                            }

                            Matrix matrix = new Matrix();
                            matrix.postScale(scaleWidth, scaleHeight);

                            // recreate the new Bitmap
                            bitmap = Bitmap.createBitmap(bitmap_orig, 0, 0, width, height, matrix, true);
                            
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            
                            if (resolver.getType(content) == "image/gif")
                            {
                                Log.i("Jetty", "Original image was gif, exporting thumb as JPEG as workaround");
                                response.setContentType("image/gif");
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                            }
                            else
                            {
                                Log.i("Jetty", "Exporting thumb in png format");
                                response.setContentType("image/png");
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                            }
                            
                            stream = new ByteArrayInputStream(bytes.toByteArray());
                        }
                        else
                        {
                            if (height == 0 || width == 0)
                                Log.w("Jetty", "Height or width were 0; sending original image instead!");
                            else
                                Log.i("Jetty", "Original was smaller than " + __THUMB_HEIGHT + "x" + __THUMB_WIDTH + ", skipping scale.");
                            // just return the original data from the DB
                            response.setContentType(resolver.getType(content));
                            stream = resolver.openInputStream(content);
                        }

                        IO.copy(stream,os);
                    }
                }
                else
                {
                    Log.i("Jetty", "Exporting original media");
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
        else if ("embed".equals(what.trim()))
        {
            String path = "/console/media/db/fetch/" + typestr + "/" + item;
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");
            PrintWriter writer = response.getWriter();
            
            writer.println ("<OBJECT ID='MediaPlayer' WIDTH='320' HEIGHT='26' CLASSID='CLSID:22D6F312-B0F6-11D0-94AB-0080C74C7E95' STANDBY='Loading...' TYPE='application/x-oleobject'>");
            writer.println ("  <PARAM NAME='FileName' VALUE=" + path + ">");
            writer.println ("  <EMBED TYPE='application/x-mplayer2' SRC='" + path + "' NAME='MediaPlayer' WIDTH='320' HEIGHT='26' autostart='1'></EMBED>");
            writer.println ("</OBJECT>");
        }
        else
        {
            Log.w("Jetty", "invalid action - '" + what + "', returning 404");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
