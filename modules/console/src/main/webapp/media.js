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
                    $("#" + type).empty();
                    $("#" + type).append ("Loading...");
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
                    errormsg = "GET media list for " + type + " failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception;
                    console.log(errormsg);
                    $("#" + type).empty();
                    $("#" + type).append ("<strong>Failed to get " + type + "!</strong><br />Error:<br /><pre>" + errormsg + "</pre>");
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
            
            html = "";
            
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
                    html += "<div class='float'><a href='/console/media/db/fetch/" + item.type + "/" + item.id + "'><div class='thumb'>&nbsp;<img src='/console/media/db/fetch/" + item.type + "/" + item.id + "/thumb/' alt='" + item.title + "'/>&nbsp;</div></a><p>" + item.title + "</p></div>";
                } else if (type == "audio") {
                    html += "<div class='float'><a href='/console/media/db/fetch/" + item.type + "/" + item.id + "' onclick='return playMedia(this);'><div class='thumb'>&nbsp;<img src='/console/audio.png' alt='" + item.title + "'/>&nbsp;</div></a><p>" + item.title;
                    
                    if (item.artist != null || item.album != null) {
                        html += "<span class='trackinfo'>";
                    }
                    
                    if (item.artist != null) {
                        if (item.artist.length == 0) {
                            html += "<br />Unknown Artist";
                        } else {
                            html += "<br />" + item.artist;
                        }
                    }
                    
                    if (item.album != null) {
                        if (item.album.length == 0) {
                            html += "<br />Unknown Album";
                        } else {
                            html += "<br />" + item.album;
                        }
                    }
                    
                    if (item.artist != null || item.album != null) {
                        html += "</span>";
                    }
                    
                    html += "</p></div>";
                } else {
                    html += "<li><a href='/console/media/db/fetch/" + item.type + "/" + item.id + ">" + item.title + "</a></li>";
                }
            }
            
            if (type == "images" || type == "audio") {
                html += "<div class='spacer'>&nbsp;</div>";
            } else {
                html += "</ul>";
            }
            
            parent.append (html);
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

function reloadMedia (type) {
    Media.getMedia(type);
    return false;
}

function stopMedia(elem) {
    $("#hidden-target")[0].src = "about:blank";
    elem.childNodes[0].childNodes[1].src = "/console/audio.png";
    document.currentPlaying = null;
    return false;
}

document.currentPlaying = null;

function playMedia (elem) {
    if (document.currentPlaying == elem) {
        stopMedia(document.currentPlaying);
        return false;
    } else if (document.currentPlaying != null) {
        stopMedia(document.currentPlaying);
    }
    
    console.log ('elem = ', elem);
    var href = elem.href;
    href = href.replace ("fetch", "embed"); // go to embed url location for iframe
    $("#hidden-target")[0].src = href;
    
    // music should start loading now, so change the image to a stop button
    elem.childNodes[0].childNodes[1].src = "/console/stop.png";
    document.currentPlaying = elem;
    return false;
}
