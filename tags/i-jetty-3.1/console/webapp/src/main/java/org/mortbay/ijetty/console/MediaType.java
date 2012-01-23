package org.mortbay.ijetty.console;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.MediaStore;

public final class MediaType
{ 
    private static Map<String, Uri[]> mediaMap = new HashMap<String,Uri[]>();
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_IMAGES = "image";
    public static final String LOCATION_EXTERNAL = "external";
    public static final String LOCATION_INTERNAL = "internal";

    static
    {
        //image
        mediaMap.put(TYPE_IMAGES, new Uri[]{MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.INTERNAL_CONTENT_URI});
        //audio
        mediaMap.put(TYPE_AUDIO, new Uri[] {MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.INTERNAL_CONTENT_URI});
        //video
        mediaMap.put(TYPE_VIDEO, new Uri[] {MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.INTERNAL_CONTENT_URI});
    }
    
    
    public static Uri getContentUriByType(String mediatype, String location)
    {
        Uri[] uris = mediaMap.get(mediatype);
        if (uris == null)
            return null;

        if (LOCATION_EXTERNAL.equalsIgnoreCase(location.trim()))
            return uris[0];
        if (LOCATION_INTERNAL.equalsIgnoreCase(location.trim()))
            return uris[1];

        return null;
    }

    /**
     * Fetch the list of content uris representing the basic media type.
     *
     * @param mediaType
     *            the basic media type to fetch
     * @return 2 Uri's representing the Content URIs for the [external, internal] content.
     */
    public static Uri[] getContentUrisByType(String mediaType)
    {
        return mediaMap.get(mediaType);
    }

}
