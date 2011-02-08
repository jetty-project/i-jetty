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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.IO;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Config;
import android.util.Log;

public class MediaBrowserServlet extends HttpServlet
{
    public class MyMediaConnectorClient implements MediaScannerConnectionClient
    {
        private MediaScannerConnection _scanner = null;
        private final File _file;

        public MyMediaConnectorClient(File file)
        {
            _file = file;
        }

        public void onMediaScannerConnected()
        {
            _scanner.scanFile(_file.getAbsolutePath(),null);
        }

        public void onScanCompleted(String path, Uri uri)
        {
            Log.i(TAG,"Finished scanning!");
            _scanner.disconnect();
        }

        public void setScanner(MediaScannerConnection scanner)
        {
            _scanner = scanner;
        }
    }
    class PathTokenizer
    {
        private StringTokenizer tok;
        private String pathinfo;
        private int location;

        public PathTokenizer(String pathinfo)
        {
            this.pathinfo = pathinfo;
            this.tok = new StringTokenizer(pathinfo,"/");
            this.location = 0;
        }

        public String getPathinfo()
        {
            return pathinfo;
        }

        public String nextOptPathSegment() throws ParseException
        {
            if (tok.hasMoreTokens())
            {
                location++;
                String ret = tok.nextToken();
                if (ret == null)
                {
                    return null;
                }
                return ret.trim();
            }
            return null;
        }

        public String nextPathSegment() throws ParseException
        {
            if (tok.hasMoreTokens())
            {
                location++;
                String ret = tok.nextToken();
                if (ret == null)
                {
                    return null;
                }
                return ret.trim();
            }
            throw new ParseException("Missing required path segment",location);
        }
    }

    private static final long serialVersionUID = 1L;
    private static final String TAG = "MediaBrowserServlet";

    private static final String TYPE_VIDEO = "video";
    private static final String TYPE_AUDIO = "audio";
    private static final String TYPE_IMAGES = "images";
    private static final String LOCATION_EXTERNAL = "external";
    private static final String LOCATION_INTERNAL = "internal";

    private int __THUMB_WIDTH = 120;
    private int __THUMB_HEIGHT = 120;
    private ContentResolver resolver;
    private Context context;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null)
        {
            Log.w(TAG,"pathInfo was null, returning 404");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (Config.LOGD)
        {
            Log.d(TAG,"PathInfo: " + pathInfo);
        }

        PathTokenizer pathtok = new PathTokenizer(pathInfo);
        try
        {
            String action = pathtok.nextPathSegment(); // 'json', 'fetch', or 'embed'

            if ("fetch".equals(action))
            {
                String type = pathtok.nextPathSegment();
                String location = pathtok.nextPathSegment();
                String item = pathtok.nextPathSegment();
                String thumb = pathtok.nextOptPathSegment();
                Uri contenturi = getContentUriByType(type,location);
                if (contenturi == null)
                {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                boolean asThumb = "thumb".equals(thumb);
                doGetFetchMedia(request,response,contenturi,item,asThumb);
            }
            else if ("json".equals(action.trim()))
            {
                String type = pathtok.nextPathSegment();
                doGetJson(request,response,type);
            }
            else if ("embed".equals(action.trim()))
            {
                String type = pathtok.nextPathSegment();
                String location = pathtok.nextPathSegment();
                String item = pathtok.nextPathSegment();
                doGetEmbedHtml(request,response,type,location,item);
            }
            else
            {
                Log.w(TAG,"invalid action - '" + action + "', returning 404");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch (ParseException e)
        {
            Log.w(TAG,"Invalid request path: " + pathInfo,e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Get the HTML snippet for embedding the media content in a way suitable for the browser to view the content as
     *
     * @param request
     *            the incoming servlet request
     * @param response
     *            the outgoing servlet response
     * @param type
     *            the type of media interested in (<code>images</code>, <code>audio</code>, or <code>video</code>)
     * @param location
     *            the location of media interested in (<code>internal</code>, or <code>external</code>)
     * @param item
     *            the item reference
     * @throws ServletException
     * @throws IOException
     */
    public void doGetEmbedHtml(HttpServletRequest request, HttpServletResponse response, String type, String location, String item) throws ServletException,
            IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();

        String path = "/console/media/db/fetch/" + type + "/" + location + "/" + item;

        writer.print("<OBJECT ID='MediaPlayer' WIDTH='320' HEIGHT='26'");
        writer.println(" CLASSID='CLSID:22D6F312-B0F6-11D0-94AB-0080C74C7E95'");
        writer.println(" STANDBY='Loading...' TYPE='application/x-oleobject'>");
        writer.printf("  <PARAM NAME='FileName' VALUE='%s'>%n",path);
        writer.printf("  <EMBED TYPE='application/x-mplayer2' SRC='%s' NAME='MediaPlayer' WIDTH='320' HEIGHT='26' autostart='1'></EMBED>%n",path);
        writer.println("</OBJECT>");
    }

    public void doGetFetchMedia(HttpServletRequest request, HttpServletResponse response, Uri contenturi, String item, boolean asThumb)
            throws ServletException, IOException
    {
        try
        {
            Uri content = Uri.withAppendedPath(contenturi,item);

            if (asThumb)
            {
                Bitmap bitmap_orig = MediaStore.Images.Media.getBitmap(resolver,content);
                if (bitmap_orig != null)
                {
                    response.setStatus(HttpServletResponse.SC_OK);
                    InputStream stream = null;
                    OutputStream os = response.getOutputStream();
                    Bitmap bitmap = null;

                    int width = bitmap_orig.getWidth();
                    int height = bitmap_orig.getHeight();

                    // If the image is too big (AND the width/height isn't 0), scale it
                    if (((width > __THUMB_WIDTH) || (height > __THUMB_HEIGHT)) && (height != 0) && (width != 0))
                    {
                        float scaleWidth = 0;
                        float scaleHeight = 0;

                        if (Config.LOGD)
                        {
                            Log.d(TAG,"orig height = " + height + ", orig width = " + width);
                            Log.d(TAG,"__THUMB_HEIGHT = " + __THUMB_HEIGHT + ", __THUMB_WIDTH = " + __THUMB_WIDTH);
                        }

                        if (width > __THUMB_WIDTH)
                        {
                            scaleWidth = ((float)__THUMB_WIDTH) / width;
                        }

                        if (height > __THUMB_HEIGHT)
                        {
                            scaleHeight = ((float)__THUMB_HEIGHT) / height;
                        }

                        if (Config.LOGD)
                        {
                            Log.d(TAG,"scaleHeight = " + scaleHeight + ", scaleWidth = " + scaleWidth);
                        }

                        if ((scaleHeight < scaleWidth) && (scaleHeight != 0))
                        {
                            scaleWidth = scaleHeight;
                        }
                        else if ((scaleWidth < scaleHeight) && (scaleWidth != 0))
                        {
                            scaleHeight = scaleWidth;
                        }

                        if (scaleWidth == 0)
                        {
                            scaleWidth = scaleHeight;
                        }

                        if (scaleHeight == 0)
                        {
                            scaleHeight = scaleWidth;
                        }

                        if (Config.LOGD)
                        {
                            Log.d(TAG,"scaleHeight = " + scaleHeight + ", scaleWidth = " + scaleWidth + " (final)");
                        }

                        if (scaleHeight == 0)
                        {
                            Log.w(TAG,"scaleHeight and scaleWidth both = 0! Setting scale to 50%.");
                            scaleHeight = 0.5f;
                            scaleWidth = 0.5f;
                        }

                        Matrix matrix = new Matrix();
                        matrix.postScale(scaleWidth,scaleHeight);

                        // recreate the new Bitmap
                        bitmap = Bitmap.createBitmap(bitmap_orig,0,0,width,height,matrix,true);

                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

                        if (resolver.getType(content) == "image/gif")
                        {
                            Log.i(TAG,"Original image was gif, exporting thumb as JPEG as workaround");
                            response.setContentType("image/gif");
                            bitmap.compress(Bitmap.CompressFormat.JPEG,90,bytes);
                        }
                        else
                        {
                            Log.i(TAG,"Exporting thumb in png format");
                            response.setContentType("image/png");
                            bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);
                        }

                        stream = new ByteArrayInputStream(bytes.toByteArray());
                    }
                    else
                    {
                        if ((height == 0) || (width == 0))
                        {
                            Log.w(TAG,"Height or width were 0; sending original image instead!");
                        }
                        else
                        {
                            Log.i(TAG,"Original was smaller than " + __THUMB_HEIGHT + "x" + __THUMB_WIDTH + ", skipping scale.");
                        }
                        // just return the original data from the DB
                        response.setContentType(resolver.getType(content));
                        stream = resolver.openInputStream(content);
                    }

                    IO.copy(stream,os);
                }
            }
            else
            {
                Log.i(TAG,"Exporting original media");
                response.setContentType(resolver.getType(content));
                InputStream stream = resolver.openInputStream(content);
                OutputStream os = response.getOutputStream();

                response.setStatus(HttpServletResponse.SC_OK);
                IO.copy(stream,os);
            }
        }
        catch (Exception e)
        {
            Log.w(TAG,"Failed to fetch media",e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * A request from the Javascript asking for some JSON describing the type of media being requested.
     *
     * @param request
     *            the incoming servlet request
     * @param response
     *            the outgoing servlet response
     * @param mediatype
     *            the type of media interested in (<code>images</code>, <code>audio</code>, or <code>video</code>)
     * @throws ServletException
     * @throws IOException
     */
    public void doGetJson(HttpServletRequest request, HttpServletResponse response, String mediatype) throws ServletException, IOException
    {
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();

        Uri[] mediaUris = getContentUrisByType(mediatype);

        writer.print("[ ");

        StringBuilder entry = new StringBuilder();

        int length = mediaUris.length;
        for (int iuri = 0; iuri < length; iuri++)
        {
            Uri contenturi = mediaUris[iuri];
            Log.d(TAG,"Using contenturi: " + contenturi);
            Cursor cur = resolver.query(contenturi,null,null,null,null);
            String location = "";
            if (contenturi.toString().contains("/internal/"))
            {
                location = LOCATION_INTERNAL;
            }
            else if (contenturi.toString().contains("/external/"))
            {
                location = LOCATION_EXTERNAL;
            }
            else
            {
                Log.w(TAG,"Unknown content uri location (not internal or external?): " + contenturi);
                continue; // skip (don't know how to handle)
            }

            while (cur.moveToNext())
            {
                Long rowid = cur.getLong(cur.getColumnIndexOrThrow(BaseColumns._ID));
                String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE));
                String displayname = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                String mimetype = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
                String size = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));

                entry.setLength(0); // clear buffer.

                entry.append("{");
                entry.append(" 'type':").append(safeJson(mediatype));
                entry.append(",'location':").append(safeJson(location));
                entry.append(",'id':").append(safeJson(rowid));
                entry.append(",'title':").append(safeJson(title));
                entry.append(",'displayname':").append(safeJson(displayname));
                entry.append(",'mimetype':").append(safeJson(mimetype));
                entry.append(",'size':").append(safeJson(size));

                if ((contenturi == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) || (contenturi == MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
                {
                    int music = cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_MUSIC));
                    if (music != 0)
                    {
                        String artist = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
                        String album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));

                        entry.append(",'artist':").append(safeJson(artist));
                        entry.append(",'album':").append(safeJson(album));
                    }
                }

                entry.append("}");
                Log.d(TAG,entry.toString());
                writer.print(entry.toString());

                if (!cur.isLast())
                {
                    writer.print(",");
                }
            }
        }

        writer.print(" ]");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter writer = response.getWriter();

        // Should we use just the servlet directory instead?
        File externalDir = Environment.getExternalStorageDirectory();
        File sdcarddir = new File(externalDir,"/jetty/media");

        // Create file upload directory if it doesn't exist
        if (!sdcarddir.exists())
        {
            sdcarddir.mkdir();
        }

        File output = null;

        try
        {
            // Save file to External Storage
            File file = (File)request.getAttribute("fileupload");
            String origName = request.getParameter("fileupload");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IO.copy(new FileInputStream(file),out);

            output = new File(sdcarddir,origName);
            Log.i(TAG,"Writing to: " + output);
            FileOutputStream stream = new FileOutputStream(output);

            out.writeTo(stream);
            stream.close();
        }
        catch (Exception e)
        {
            Log.w(TAG,"Failed to save uploaded file",e);
            printResponse(writer,1,"Could not save uploaded file to sdcard.",-1);
            return;
        }

        // Re-run media scanner, to re-detect media
        MyMediaConnectorClient client = new MyMediaConnectorClient(output);
        MediaScannerConnection scanner = new MediaScannerConnection(context,client);
        client.setScanner(scanner);

        scanner.connect();

        int filetype = -1;
        printResponse(writer,0,"No error",filetype);

    }

    public ContentResolver getContentResolver()
    {
        return resolver;
    }

    private Uri getContentUriByType(String mediatype, String location)
    {
        // Try by name
        if (TYPE_IMAGES.equals(mediatype))
        {
            if(LOCATION_INTERNAL.equals(location)) {
                return MediaStore.Images.Media.INTERNAL_CONTENT_URI;
            } else if(LOCATION_EXTERNAL.equals(location)) {
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else {
                Log.w(TAG,"Unknown location [" + location + "] for type [" + mediatype + "]");
                return null;
            }
        }
        else if (TYPE_AUDIO.equals(mediatype))
        {
            if(LOCATION_INTERNAL.equals(location)) {
                return MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
            } else if(LOCATION_EXTERNAL.equals(location)) {
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else {
                Log.w(TAG,"Unknown location [" + location + "] for type [" + mediatype + "]");
                return null;
            }
        }
        else if (TYPE_VIDEO.equals(mediatype))
        {
            if(LOCATION_INTERNAL.equals(location)) {
                return MediaStore.Video.Media.INTERNAL_CONTENT_URI;
            } else if(LOCATION_EXTERNAL.equals(location)) {
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else {
                Log.w(TAG,"Unknown location [" + location + "] for type [" + mediatype + "]");
                return null;
            }
        }

        // Not a known name either. fail.
        Log.w(TAG,"Type String [" + mediatype + "] not a known ContentUri reference");
        return null;
    }

    /**
     * Fetch the list of content uris representing the basic media type.
     *
     * @param mediatype
     *            the basic media type to fetch
     * @return 2 Uri's representing the Content URIs for the [external, internal] content.
     */
    private Uri[] getContentUrisByType(String mediatype)
    {
        // Try by name
        if (TYPE_IMAGES.equals(mediatype))
        {
            return new Uri[]
            { MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.INTERNAL_CONTENT_URI };
        }
        else if (TYPE_AUDIO.equals(mediatype))
        {
            return new Uri[]
            { MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.INTERNAL_CONTENT_URI };
        }
        else if (TYPE_VIDEO.equals(mediatype))
        {
            return new Uri[]
            { MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.INTERNAL_CONTENT_URI };
        }

        // Not a known name either. fail.
        Log.w(TAG,"Type String [" + mediatype + "] not a known ContentUri reference");
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
        context = (Context)getServletContext().getAttribute("org.mortbay.ijetty.context");
    }

    private void printResponse(PrintWriter writer, int resp, String msg, int filetype)
    {
        writer.println("<script>");
        writer.println("var json = { error: " + resp + ", msg: '" + msg + "', filetype: " + filetype + " };");
        writer.println("if (top.Media) { top.Media.uploadComplete(json); }");
        writer.println("</script>");
    }

    private String safeJson(Number num)
    {
        if (num == null)
        {
            return "''";
        }
        return num.toString();
    }

    private String safeJson(String str)
    {
        if (str == null)
        {
            return "''";
        }
        return "'" + str.replaceAll("'","\\'") + "'";
    }
}
