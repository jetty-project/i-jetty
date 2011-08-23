package org.mortbay.ijetty.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ContentResolver;
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
    

    
   
    
    private ContentResolver resolver;
    private Context context;
    
    
    
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
            //Get metadata about a specific media item
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
        }
        else
        {
            //Get metadata about all media items of a certain type
            Uri[] mediaUris = MediaType.getContentUrisByType(type);

            writer.print("[ ");

            StringBuilder builder = new StringBuilder();

            int length = mediaUris.length;
            for (int iuri = 0; iuri < length; iuri++)
            {
                Uri contenturi = mediaUris[iuri];
                Log.d(TAG,"Using contenturi: " + contenturi);

                Cursor cur = null;

                try
                {
                    cur = resolver.query(contenturi,null,null,null, MediaStore.MediaColumns.TITLE+" ASC");

                    if (cur == null)
                        continue; //skip - no results?

                    while (cur.moveToNext())
                    {  
                        builder.setLength(0); // clear buffer
                        String str = contenturi.toString();
                        location = null;
                        if (str.contains("/internal/"))
                            location = MediaType.LOCATION_INTERNAL;
                        else if (str.contains("/external/"))                      
                            location = MediaType.LOCATION_EXTERNAL;

                        toJson(cur, contenturi, builder, type, location);
                        Log.d(TAG,builder.toString());
                        writer.print(builder.toString());

                        if (!cur.isLast())
                        {
                            writer.print(",");
                        }
                    }
                }
                finally
                {
                    if (cur != null)
                        cur.close();
                }
            }

            writer.print(" ]");
        }
    }



    private void toJson(Cursor cur, Uri contenturi, StringBuilder builder, String type, String location)
    {
        Long rowid = cur.getLong(cur.getColumnIndexOrThrow(BaseColumns._ID));
        String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE));
        String displayname = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
        String mimetype = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
        String size = cur.getString(cur.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));


        builder.append("{");
        builder.append(" 'type':").append(safeJson(type));
        builder.append(",'location':").append(safeJson(location));
        builder.append(",'id':").append(safeJson(rowid));
        builder.append(",'title':").append(safeJson(title));
        builder.append(",'displayname':").append(safeJson(displayname));
        builder.append(",'mimetype':").append(safeJson(mimetype));
        builder.append(",'size':").append(safeJson(size));

        if ((contenturi == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) || (contenturi == MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
        {
            int music = cur.getInt(cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_MUSIC));
            if (music != 0)
            {
                String artist = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
                String album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));

                builder.append(",'artist':").append(safeJson(artist));
                builder.append(",'album':").append(safeJson(album));
            }
        }

        builder.append("}");
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
