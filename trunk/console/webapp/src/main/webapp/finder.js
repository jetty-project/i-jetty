/*
             {
                location: { lat: x, long: y}
             }
             
             or
             
             { error : msg }
*/
var Finder = 
{

	tracking: false,
	tracks: [],
    location: {},
    
    
         update: function (successfn, errfn)
         {
        	 var uri =  "/console/rest/finder/?action=update";
             $.ajax({
                 type: 'GET',
                 url: uri,
                 dataType: 'json',
                 beforeSend: function(xhr)
                 {
                     return true;
                 },
                 success: function(response) 
                 { 
                     if (!successfn)
                     {
                     	if (response.error)
                     	{
                     	    if ("Not tracking" == response.error)
                     	        Finder.stopTrackLocation(); //stop tracking on ui
                     		Finder.renderError(response.error);
                     	}
                     	else
                     	{
                     		if (response.location)
                     		{
                     		    if (response.location.lat != Finder.location.lat &&
                     		        response.location.long != Finder.location.long &&
                     		        response.location.time != Finder.location.time)
                     		    {
                     		        Finder.location = response.location;
                     		        Finder.tracks.push(Finder.location);
                     		    }
                                Finder.renderLocation(); 
                     		}
                     	}
                     }
                     else
                         successfn(response);
                 },
                 error: function(xhr, reason, exception) 
                 {   
                     /*
                     if (xhr.getResponseHeader("Content-Type") == "text/html" || xhr.status == 0)
                         window.location.reload();
                     else
                         alert("Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                     */ 
                     Finder.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try reloading the page.");
                 }
             });
             return false; 
         },
        
        lastLocation: function (successfn, errfn)
        {
        	 var uri =  "/console/rest/finder/?action=last";
             $.ajax({
                 type: 'GET',
                 url: uri,
                 dataType: 'json',
                 beforeSend: function(xhr)
                 {
                     return true;
                 },
                 success: function(response) 
                 { 
                     if (!successfn)
                     {
                     	if (response.error)
                     		Finder.renderError(response.error);
                     	else
                     	{
                     		if (response.location)
                     		{
                     			Finder.location = response.location;
                     			Finder.renderLocation(); 
                     		}
                     	}
                     }
                     else
                         successfn(response);
                 },
                 error: function(xhr, reason, exception) 
                 {             	
                     Finder.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try reloading the page.");
                 }
             });
             return false; 
        },
        
        stopTrack: function (successfn, errfn)
        {
        	var uri = "/console/rest/finder/?action=stopTrack";
        	 $.ajax({
                 type: 'POST',
                 url: uri,
                 dataType: 'json',
                 beforeSend: function(xhr)
                 {
                     return true;
                 },
                 success: function(response) 
                 { 
                     if (!successfn)
                     {
                     	if (response.error && response.error != 200)
                     		alert(response.error);
                     }
                     else
                         successfn(response);
                 },
                 error: function(xhr, reason, exception) 
                 {                       
                     Finder.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try reloading the page.");
                 }
             });
             return false; 
        },
        
        startTrack: function (successfn, errfn)
        {
            var uri =  "/console/rest/finder/?action=track";
            $.ajax({
                type: 'POST',
                url: uri,
                dataType: 'json',
                beforeSend: function(xhr)
                {
                    return true;
                },
                success: function(response) 
                { 
                    if (!successfn)
                    {
                    	if (response.error)
                    		Finder.renderError(response.error);
                    	else
                    	{
                    	    if (response.location)
                    	    {
                    	        if (response.location.lat != Finder.location.lat &&
                    	                response.location.long != Finder.location.long &&
                    	                response.location.time != Finder.location.time)
                    	        {
                    	            Finder.location = response.location;
                    	            Finder.tracks.push(Finder.location);
                    	            Finder.renderLocation(); 
                    	        }
                    	    }

                    	}
                    }
                    else
                        successfn(response);
                },
                error: function(xhr, reason, exception) 
                {  
                    Finder.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try reloading the page.");
                }
            });
            return false; 
        },
        
        showLastLocation: function ()
        {
            $("#location").html("<img src='../spinner.gif'/>");
            Finder.lastLocation();
        },
        
        trackLocation: function ()
        {
        	if (Finder.tracking)
        		return; //already tracking
        	$("#location").html("");
        	$("#location").html("<img src='../spinner.gif'/>");
        	Finder.tracks = [];
        	Finder.location = {};
        	Finder.tracking = true;
        	$("#last").attr("disabled", "true");
        	$("#up").removeAttr("disabled");
        	$("#start").attr("disabled", "true");
        	$("#stop").removeAttr("disabled");
        	Finder.startTrack();
        },
        
        stopTrackLocation: function ()
        {
        	Finder.tracks = [];
        	Finder.location = {};
        	Finder.tracking = false;
        	$("#location").html("");
        	Finder.stopTrack();
        	$("#last").removeAttr("disabled");
        	$("#up").attr("disabled", "true");
        	$("#start").removeAttr("disabled");
        	$("#stop").attr("disabled", "true");
        },
        
        updateTrackLocation: function ()
        {
            $("#location").html("<img src='../spinner.gif'/>"); 
            Finder.update();
        },
        
        renderError: function(err)
        {
            $("#location").html("<p class='error'>"+err+"</p>"); //get rid of any previous location
            //$("#up").attr("disabled", "true"); 
            //$("#start").remove("disabled", "true");
            //$("#stop").attr("disabled", "true");
            //$("#ring").attr("disabled", "true");
        },
        
        
        renderLocation: function ()
        {
            $("#location").html(""); //get rid of any previous location
            var html = ""; 
            html += "<h3>Device Location</h3>";
            html += "<p>";
            html += "<b>Latitude:</b>&nbsp;"+Finder.location.lat;
            html += "&nbsp;<b>Longitude:</b>&nbsp;"+Finder.location.long;
            html+= "<br/><b>At local time:</b>&nbsp;"+new Date(Finder.location.time).toLocaleString();
            html+="</p>";
         

            /* Android image that is accessible from the net at
             * http://tinyurl.com/3gvyf2f
             * 
             */
            if (Finder.location)
            {
                var url = "http://maps.googleapis.com/maps/api/staticmap?";
                url += "size=512x512&sensor=false&maptype=hybrid";
                url += "&markers=icon:http://tinyurl.com/3gvyf2f%7c"+Finder.location.lat+","+Finder.location.long;
                if (Finder.tracks && Finder.tracks.length > 0)
                {
                    url += "&path=color:0xa4c639%7c";
                    for (var i=0;i<Finder.tracks.length;i++)
                    {
                        url += Finder.tracks[i].lat+","+Finder.tracks[i].long;
                        if (i < Finder.tracks.length -1)
                            url += "%7c";
                    }
                }
                else
                    url += "&zoom=15";
            }
            html += "<img id=\"map\" class=\"map\"/>";

            $("#location").append(html);
            $("#map")[0].src = url;
        }
  
};




$(document).ready (function () {

}); 
