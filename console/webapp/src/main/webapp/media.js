var Media = 
{
        media: {
            "image" : {"internal" : null, "external":null},        
            "audio" : {"internal" : null, "external":null},
            "video" : {"internal" : null, "external":null}
        },
        
		page_size : 10,
		
		pages: {
			"image": {"internal": 0, "external":0},
			"audio": {"internal": 0, "external":0},
			"video": {"internal": 0, "external":0}
		},
	
		totals : {
			"image": {"internal": 0, "external":0},
			"audio": {"internal": 0, "external":0},
			"video": {"internal": 0, "external":0}
		},
		
		first: function (type, location)
		{
			Media.pages[type][location] = 0;
			Media.getMedia(type, location);
		},
		
        prev:  function (type, location)
        {
        	Media.pages[type][location] = Media.pages[type][location] - Media.page_size;
        	//Media.page_pos = Media.page_pos - Media.page_size;
        	Media.getMedia(type, location);
        },
        
        
        next:  function (type, location)
        {
        	Media.pages[type][location] = Media.pages[type][location] + Media.page_size;
        	Media.getMedia(type, location);
        },
        
        getMedia: function (type, location)
        {
            var uri =  "/console/rest/media/" + type + "/" + location;
            uri += "/?pgStart="+Media.pages[type][location]+"&pgSize="+Media.page_size;
            
            $.ajax({
                type: 'GET',
                url: uri,
                dataType: 'json',
                beforeSend: function(xhr)
                {
                    $("#" + type+"-"+location).empty();
                    $("#" + type+"-"+location).append ("Loading...");
                    return true;
                },
                success: function(response) 
                {
                    Media.media[type][location] = response.media;
                    Media.totals[type][location] = response.total;
                    Media.renderMedia(type, location);
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
            	for (var location in Media.media[type])
            		Media.getMedia(type,location);
            }
                
        },
        
        renderMedia: function (type, location)
        {
            var lmedia = Media.media[type][location];
            parent = $("#" + type+"-"+location);
            parent.empty ();
            
            html = "";
            
            if (type == "video")
                parent.append ("<ul>");
            
            // inner content
            for (var itemidx in lmedia)
            {
                var item = lmedia[itemidx];
                
                if (type == "image")
                {
                    html += "<div class='float'><a href='/console/browse/media/" + item.type + "/" + item.location + "/" + item.id + 
                          "'><div class='thumb'>&nbsp;<img src='/console/browse/media/" + item.type + "/" + item.location + "/thumb/" + item.id + "' alt='" + 
                          item.title + "'/>&nbsp;</div></a><p>" + item.title + "</p></div>";
                }
                else if (type == "audio")
                {
                    html += "<div class='float'><a href='/console/browse/media/" + item.type + "/" + item.location + "/" + item.id + 
                    	  "' onclick='return playMedia(this);'><div class='thumb'>&nbsp;<img src='/console/audio.png' alt='" + item.title + "'/>&nbsp;</div></a><p>" + item.title;
                    
                    if (item.artist != null || item.album != null)
                        html += "<span class='trackinfo'>";
                    
                    if (item.artist != null)
                    {
                        if (item.artist.length == 0)
                            html += "<br />Unknown Artist";
                        else
                            html += "<br />" + item.artist;
                    }
                    
                    if (item.album != null)
                    {
                        if (item.album.length == 0)
                            html += "<br />Unknown Album";
                        else
                            html += "<br />" + item.album;
                    }
                    
                    if (item.artist != null || item.album != null)
                        html += "</span>";
                    
                    html += "</p></div>";
                } 
                else 
                {
                    html += "<li><a href='/console/browse/media/" + item.type + "/" + item.location+ "/" + item.id + "'>" + item.title + "</a></li>";
                }
            }
            
            if (type == "video")
                html += "</ul>";
            
            html += "<div class='buttons'>";
            
            var disabled = (Media.pages[type][location] > 0 && Media.media[type][location].length > 0)?"":"disabled";
            html += "<button onclick='Media.first(\""+type+"\",\""+location+"\");'"+disabled+">First Page</button>";
            
            var disabled = (Media.pages[type][location] > 0 && Media.media[type][location].length > 0)?"":"disabled";
            html += "<button  onclick='Media.prev(\""+type+"\",\""+location+"\");' "+disabled+">Prev Page</button>";


            disabled = (Media.totals[type][location] > (Media.pages[type][location] + Media.page_size))?"":"disabled";
            html += "<button  onclick='Media.next(\""+type+"\",\""+location+"\");' "+disabled+">Next Page</button>";
            
            html += "</div>";
            
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
            Media.getAllMedia();
            
            $("#fileupload").attr("value", "")
        }
};



$(document).ready (function () {
    Media.getAllMedia();
}); 

function reloadMedia (type) {
    Media.getMedia(type, "internal");
    Media.getMedia(type, "external");
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
    href = href.append ("?action=embed"); //TODO REPLACE REPLACE go to embed url location for iframe
    $("#hidden-target")[0].src = href;

    
    // music should start loading now, so change the image to a stop button
    elem.childNodes[0].childNodes[1].src = "/console/stop.png";
    document.currentPlaying = elem;
    return false;
}
