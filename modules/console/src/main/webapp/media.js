var Media = 
{
        media: {
            "images" : null,
            "audio"  : null,
            "video"  : null
        },
        
        getMedia: function (type)
        {
            var uri =  "/console/media/db/json/" + type + "/";
            console.log("uri = "+uri);
            $.ajax({
                type: 'GET',
                url: uri,
                dataType: 'json',
                beforeSend: function(xhr)
                {
                    xhr.setRequestHeader('Connection', 'Keep-Alive');
                    return true;
                },
                success: function(response) 
                {
                    console.log("Got list of media (of type '" + type + "')");
                    Media.media[type] = response;
                    Media.renderMedia(type);
                },
                error: function(xhr, reason, exception) 
                { 
                    console.log("GET media list failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                }
            });
            return false; 
        },
        
        getAllMedia: function()
        {
            for (var type in Media.media)
            {
                console.log ("fetching all " + type);
                Media.getMedia (type);
            }
        },
        
        renderMedia: function (type)
        {
            media = Media.media[type];
            parent = $("#" + type);
            parent.empty ();
            
            if (media == null || media.length == 0) {
                parent.append ("<p>No " + type + " found on this phone.</p>");
                return;
            }
            
            if (type == "images" || type == "audio") {
                parent.append ("<div class='spacer'>&nbsp;</div>");
            } else {
                parent.append ("<ul>");
            }
            
            // inner content
            for (var itemidx in media)
            {
                var item = media[itemidx];
                
                if (type == "images") {
                    $("#" + type).append ("<div class='float'><a href='/console/media/db/fetch/" + item.type + "/" + item.id + "'><img src='/console/media/db/fetch/" + item.type + "/" + item.id + "/thumb/' alt='" + item.title + "'/></a><br /><p>" + item.title + "</p></div>");
                } else if (type == "audio") {
                    $("#" + type).append ("<div class='float'><a href='/console/media/db/fetch/" + item.type + "/" + item.id + "'><img src='/console/audio.png' alt='" + item.title + "'/></a><br /><p>" + item.title + "</p></div>");
                } else {
                    $("#" + type).append ("<li><a href='/console/media/db/fetch/" + item.type + "/" + item.id + ">" + item.title + "</a></li>");
                }
            }
            
            if (type == "images" || type == "audio") {
                parent.append ("<div class='spacer'>&nbsp;</div>");
            } else {
                parent.append ("</ul>");
            }
        }
};


// For browsers without logging.
if (console == null && document.console == null) {
    document.console = {
        log: function() {
            return;
        }
    };
}


$(document).ready (function () {
    Media.getAllMedia();
    console.log("document ready");
}); 
