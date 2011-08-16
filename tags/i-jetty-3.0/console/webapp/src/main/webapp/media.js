var Media = 
{
        media: {
            "images" : null,
            "audio"  : null,
            "video"  : null
        },
        
        getMedia: function (type, location)
        {
            var uri =  "/console/media/db/json/" + type + "/" + location;
            $.ajax({
                type: 'GET',
                url: uri,
                dataType: 'json',
                beforeSend: function(xhr)
                {
                    $("#" + type).empty();
                    $("#" + type).append ("Loading...");
                    // xhr.setRequestHeader('Connection', 'Keep-Alive'); // NOT POSSIBLE TO SET IN Chromium
                    return true;
                },
                success: function(response) 
                {
                    Media.media[type] = response;
                    Media.renderMedia(type);
                },
                error: function(xhr, reason, exception) 
                { 
                    errormsg = "GET media list for " + type + " failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception;
                    alert(errormsg);
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
                Media.getMedia (type, "internal");
                Media.getMedia (type, "external");
            }
        },
        
        renderMedia: function (type)
        {
            lmedia = Media.media[type];
            parent = $("#" + type);
            parent.empty ();
            
            html = "";
            
            if (lmedia == null || lmedia.length == 0) {
                parent.append ("<p>No " + type + " found on this phone.</p>");
                return;
            }
            
            if (type == "images" || type == "audio") {
                parent.append ("<div class='spacer'>&nbsp;</div>");
            } else {
                parent.append ("<ul>");
            }
            
            // inner content
            for (var itemidx in lmedia)
            {
                var item = lmedia[itemidx];
                
                if (type == "images") {
                    html += "<div class='float'><a href='/console/media/db/fetch/" + item.type + "/" + item.location + "/" + item.id + 
                          "'><div class='thumb'>&nbsp;<img src='/console/media/db/fetch/" + item.type + "/" + item.location + "/" + item.id + "/thumb/' alt='" + 
                          item.title + "'/>&nbsp;</div></a><p>" + item.title + "</p></div>";
                } else if (type == "audio") {
                    html += "<div class='float'><a href='/console/media/db/fetch/" + item.type + "/" + item.location + "/" + item.id + 
                    	  "' onclick='return playMedia(this);'><div class='thumb'>&nbsp;<img src='/console/audio.png' alt='" + item.title + "'/>&nbsp;</div></a><p>" + item.title;
                    
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
                    html += "<li><a href='/console/media/db/fetch/" + item.type + "/" + item.location+ "/" + item.id + "'>" + item.title + "</a></li>";
                }
            }
            
            if (type == "images" || type == "audio") {
                html += "<div class='spacer'>&nbsp;</div>";
            } else {
                html += "</ul>";
            }
            
            parent.append (html);
        },
        
        uploadComplete: function(json) {
            if (json.error != 0) {
                alert ("Failed to upload file to phone: " + json.msg);
                return;
            }
            
            setTimeout(this.finishUpload, 1000);
        },
        
        finishUpload: function() {
            // OK, we assume that the upload completed fine, so let's refresh the media types.
            /*if (json.filetype == -1) {
                this.getAllMedia();
            } else {
                this.getMedia (json.filetype);
            }*/
            
            this.getAllMedia();
            
            $("#fileupload").attr("value", "")
        }
};



$(document).ready (function () {
    Media.getAllMedia();
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
    
    var href = elem.href;
    href = href.replace ("fetch", "embed"); // go to embed url location for iframe
    $("#hidden-target")[0].src = href;
    
    // music should start loading now, so change the image to a stop button
    elem.childNodes[0].childNodes[1].src = "/console/stop.png";
    document.currentPlaying = elem;
    return false;
}
