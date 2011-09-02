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
                     		alert(response.error);
                     	else
                     	{
                     		Finder.location = response.location;
                     		Finder.renderLocation(); 
                     	}
                     }
                     else
                         successfn(response);
                 },
                 error: function(xhr, reason, exception) 
                 {             	
                     if (!errfn)
                     {
                         alert("Settings Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                     }
                     else
                     {
                         errfn (xhr, reason, exception);
                     }
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
                     		alert(response.error);
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
                     if (!errfn)
                     {
                         alert("Settings Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                     }
                     else
                     {
                         errfn (xhr, reason, exception);
                     }
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
                     if (!errfn)
                     {
                         alert("Settings Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                     }
                     else
                     {
                         errfn (xhr, reason, exception);
                     }
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
                    		alert(response.error);
                    	else
                    	{
                    		Finder.location = response.location;
                    		Finder.renderLocation(); 
                    	}
                    }
                    else
                        successfn(response);
                },
                error: function(xhr, reason, exception) 
                {             	
                    if (!errfn)
                    {
                        alert("Settings Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                    }
                    else
                    {
                        errfn (xhr, reason, exception);
                    }
                }
            });
            return false; 
        },
        
        trackLocation: function ()
        {
        	if (Finder.tracking)
        		return; //already tracking
        	$("#location").html("");
        	Finder.tracks = [];
        	Finder.location = {};
        	Finder.tracking = true;
        	Finder.startTrack();
        },
        
        stopTrackLocation: function ()
        {
        	Finder.tracks = [];
        	Finder.location = {};
        	Finder.tracking = false;
        	$("#location").html("");
        	Finder.stopTrack();
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
         
            
            /* TODO work out where to get an android image that is accessible from the net
            var h = document.URL;
            var i = document.URL.substring("/console");
            h = h.substring(0, i+8);
            h += "/android.jpg";
            */
            var url = "http://maps.googleapis.com/maps/api/staticmap?";
            url += "zoom=15&size=400x400&sensor=false&maptype=hybrid";
            url += "&markers=color:green%7clabel:A%7c"+Finder.location.lat+","+Finder.location.long;
            
            html += "<img id=\"map\" class=\"map\"/>";
                    
            $("#location").append(html);
            $("#map")[0].src = url;
        }
  
};




$(document).ready (function () {

}); 
