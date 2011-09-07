/*
    settings: { 
                headings: [x,y,z],
                rows: [
                        [x1,y1,z1],
                        [x2,y2,z2],
                        [x3,y3,z3]
                      ]
              };
*/
var Settings = 
{
	page_size : 10,
	page_pos : 0,
	total : 0, 
        action_none : -1,
        action_edit: 1,
        action_save: 2,
        action_ring: 3,
         
        headings: [],
        rows: [],
        
        
        getSettings: function (successfn, errfn)
        {
            var uri =  "/console/rest/settings/";
            $.ajax({
                type: 'GET',
                url: uri,
                dataType: 'json',
                beforeSend: function(xhr)
                {
                    $("#loading").css ("display", "block");
                    return true;
                },
                success: function(response) 
                { 
                    $("#loading").css ("display", "none");
                    if (!successfn)
                    {
                        Settings.headings = response.settings.headings;
                        Settings.rows = response.settings.rows;
                        Settings.renderSettings();
                        
                    }
                    else
                        successfn(response);
                },
                error: function(xhr, reason, exception) 
                {             	
                   Settings.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try reloading the page.");

                }
            });
            return false; 
        },
        
        doRing: function ()
        {
        	return true;
        },
        
        renderSettings: function ()
        {
            $("#settings").html(""); //get rid of any previous
            var html = ""; 
            $("#pg-head").html("System Settings");
            
          
            html += "<table id='stbl'><thead><tr>";
            for (var h in Settings.headings)
            {
                html+= "<th>"+Settings.headings[h]+"</th>";
            }
            html += "</tr></thead>";
            html += "<tbody id='sbody'>";
            for (var r in Settings.rows)
            {
                html += "<tr>";
 
                for (var i in Settings.rows[r])
                    html += "<td>"+Settings.rows[r][i]+"</td>";
                html+= "</tr>";
            }
            html += "</tbody></table>";
            $("#settings").html(html);
  
            // make it sortable
            $("#stbl").tablesorter({sortList: [[1,0]], headers: { 2: {sorter:false}}});
        },
        
        
        renderError: function (msg)
        {
            $("#settings").html(""); //get rid of any previous
            $("#settings").html("<p class='error'>"+msg+"</p>");
        }
  
};




$(document).ready (function () {
    Settings.getSettings();
}); 
