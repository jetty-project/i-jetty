package org.mortbay.ijetty.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Config;
import android.util.Log;
/**
 * MediaRestServlet
 *
 *  /console/rest/media/[type]/[location]/[id][?action=x]
 *  
 *  type: image, audio, video
 *  location: internal, external
 *  action: embed
 *  
 *  
 *  /console/rest/media/image/internal/3
 *  Retrieves info about image 3 from internal storage
 *  
 *  /console/rest/media/image
 *  Retrieves info on all images from internal and external storage
 *  
 *  /console/rest/media/[type]/[location]/[id]?action=embed
 *  Generates html page that embeds a media player for the media
 *  
 */
public class MediaRestServlet extends HttpServlet
{
    private static final String TAG = "MediaRstSrvlt";
    public static final String __PG_START_PARAM = "pgStart";
    public static final String __PG_SIZE_PARAM = "pgSize";
    public static final int __DEFAULT_PG_START = 0;
    public static final int __DEFAULT_PG_SIZE = 10;
    private ContentResolver resolver;
    private Context context;
    
    
    
    public class MediaCollection extends DatabaseCollection
    {
        public MediaCollection(Cursor cursor)
        {
            super(cursor);
        }
        
        /**
         * @param cursor
         * @param startPos number of rows to skip
         * @param limit number of rows to return
         */
        public MediaCollection(Cursor cursor, int startPos, int limit)
        {
            super(cursor, startPos, limit);
        }
        

        @Override
        public ContentValues cursorToValues(Cursor cursor)
        {
            ContentValues values = new ContentValues();
            String val;
            val = cursor.getString(cursor.getColumnIndex(android.provider.BaseColumns._ID));
            values.put(android.provider.BaseColumns._ID,val);

            int idx = -1;
            idx = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE);
            if (idx > -1)
                values.put(MediaStore.MediaColumns.TITLE, cursor.getString(idx));

            idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (idx > -1)
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, cursor.getString(idx));

            idx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
            if (idx > -1)
                values.put(MediaStore.MediaColumns.MIME_TYPE, cursor.getString(idx));

            idx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
            if (idx > -1)
                values.put(MediaStore.MediaColumns.SIZE, cursor.getString(idx));

            idx = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.IS_MUSIC);
            if (idx > -1)
            {
                int music = cursor.getInt(idx);
                if (music > 0)
                {
                    idx = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
                    if (idx > -1)
                        values.put(MediaStore.Audio.AudioColumns.ARTIST,cursor.getString(idx));
                    idx = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM);
                    if (idx > -1)
                        values.put(MediaStore.Audio.AudioColumns.ALBUM, cursor.getString(idx));
                }
            }

            return values;
        }
    }
    
    
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        resolver = (ContentResolver)getServletContext().getAttribute("org.mortbay.ijetty.contentResolver");
        context = (Context)getServletContext().getAttribute("org.mortbay.ijetty.context");
    }
    
    
    
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

        String type = null;
        String location = null;
        String id = null;
        String action = null;

        StringTokenizer strtok = new StringTokenizer(pathInfo,"/");
        if (strtok.hasMoreElements())
        {
            type = strtok.nextToken();
        }

        if (strtok.hasMoreElements())
        {
            location = strtok.nextToken();
        }

        if (strtok.hasMoreElements())
        {
            id = strtok.nextToken();
        }
        
        doGetJson(request,response,type,location,id);
    }

   

    /**
     * A request from the Javascript asking for some JSON describing the type of media being requested.
     *
     * @param request
     *            the incoming servlet request
     * @param response
     *            the outgoing servlet response
     * @param type
     *            the type of media interested in (<code>image</code>, <code>audio</code>, or <code>video</code>)
     * @param location internal/external
     * @param id  optional id of specific item to retrieve
     * @throws ServletException
     * @throws IOException
     */
    public void doGetJson(HttpServletRequest request, HttpServletResponse response, String type, String location, String id) throws ServletException, IOException
    {
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();

        if (id != null)
        {
            //TODO decide if we want to support this
            //Get metadata about a specific media item
            /*
            Uri uri = MediaType.getContentUriByType(type, location);
            Uri content = Uri.withAppendedPath(uri,id);
            Cursor cur = null;
            try
            {
                cur = resolver.query(uri,null,null,null,null);
                if (cur != null && cur.getCount() == 1)
                {
                    StringBuilder builder = new StringBuilder();
                    toJson(cur, uri, builder, type, location);
                    writer.print(builder.toString());
                }
            }
            finally
            {
                if (cur != null)
                    cur.close();
            }
            */
        }
        else
        {
            //Get (a page of) media items of a certain type and location
            String tmp = request.getParameter(__PG_START_PARAM);
            int pgStart = (tmp == null ? -1 : Integer.parseInt(tmp.trim()));
            tmp = request.getParameter(__PG_SIZE_PARAM);
            int pgSize = (tmp == null ? -1 : Integer.parseInt(tmp.trim()));
                
            Uri mediaUri = MediaType.getContentUriByType(type, location);           
            StringBuilder builder = new StringBuilder();

            //Get all of the applicable collections (ie both internal and external for a given type)            
            MediaCollection collection = null;
            
            writer.println ("{");
            try
            {
                collection = new MediaCollection (resolver.query(mediaUri,null,null,null, MediaStore.MediaColumns.TITLE+" ASC"), pgStart, pgSize);
                writer.println("\"total\": "+collection.getTotal()+", ");
                writer.println("\"media\": ");
                writer.print("[ ");

                ContentValues media = null;
                int count = pgSize;

                while ((pgSize <= 0 || count-- > 0) && (media = collection.next()) != null)
                {
                    builder.setLength(0); // clear buffer
                    toJson(media, mediaUri, builder, type, location);
                    Log.d(TAG,builder.toString());
                    writer.print(builder.toString());

                    if (collection.hasNext())
                    {
                        writer.print(",");
                    }
                }
                writer.print(" ]");
            }
            finally
            {
                writer.println("}");
                collection.close();
            }
        }
    }



    private void toJson(ContentValues media, Uri contenturi, StringBuilder builder, String type, String location)
    {
        builder.append("{");
        builder.append(" \"type\":").append(safeJson(type));
        builder.append(",\"location\":").append(safeJson(location));
        builder.append(",\"id\":").append(safeJson(media.getAsInteger(BaseColumns._ID)));
        builder.append(",\"title\":").append(safeJson(media.getAsString(MediaStore.MediaColumns.TITLE)));
        builder.append(",\"displayname\":").append(safeJson(media.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)));
        builder.append(",\"mimetype\":").append(safeJson(media.getAsString(MediaStore.MediaColumns.MIME_TYPE)));
        builder.append(",\"size\":").append(safeJson(media.getAsString(MediaStore.MediaColumns.SIZE)));

        if ((contenturi == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) || (contenturi == MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
        {
            String tmp = media.getAsString(MediaStore.Audio.AudioColumns.ARTIST);
            if (tmp != null)
                builder.append(",\"artist\":").append(safeJson(tmp));

            tmp = media.getAsString(MediaStore.Audio.AudioColumns.ALBUM);
            if (tmp != null)
                builder.append(",\"album\":").append(safeJson(tmp));
        }

        builder.append("}");
    }





    private String safeJson(Number num)
    {
        if (num == null)
        {
            return "";
        }
        return num.toString();
    }

    private String safeJson(String str)
    {
        if (str == null)
        {
            return "\"\"";
        }
        return "\"" + str.replaceAll("'","\\'") + "\"";
    }
}
