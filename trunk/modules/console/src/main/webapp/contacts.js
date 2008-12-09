$(document).ready(function() {
    $(".userlink").click (function() {
        // To be fired when a field is updated.
        var submitFunction = function(value, settings) {
            // TODO: Actually grab all the form values, and send to server.
            return value;
        };
        
        var uri = $(this).attr("href") + "?json=1";
        $.getJSON(uri, function(data) {
            console.log ("JSON data from server: ", data);
            
            if (!data.summary) {
                alert ("Uh oh. Server returned bad data.");
            }
            
            // Set name and notes fields in view pane
            $("#userinfo").removeClass ("hidden");
            $("#user-name").text (data.summary.name);
            $("#user-name").editable(submitFunction, { submit: "OK" });
            
            // Setup the user photo (will just display generic picture if none set).
            $("#user-photo").attr ("src", "/console/contacts/" + data.summary.id + "/photo");
            
            if (data.summary.notes) {
                $("#user-notes-add").addClass ("hidden");
                $("#user-notes").removeClass ("hidden");
                $("#user-notes").text (data.summary.notes);
                $("#user-notes").editable(submitFunction, { type: "textarea", submit: "OK" });
                
            } else {
                // FIXME: Make add-notes button do stuff.
                $("#user-notes-add").removeClass ("hidden");
                $("#user-notes").addClass ("hidden");
                $("#user-notes").text ("");
            }
            
            $("#user-numbers").empty();
            
            if (data.phones) {
                var looped = false;
                jQuery.each(data.phones, function (number, info) {
                    looped = true;
                    
                    var label = "Unknown";
                    if (info.type == "null" && info.label && info.label != "null" && info.label.length > 0) {
                        label = info.label;
                    } else {
                        label = info.type.substr(0, 1).toUpperCase() + info.type.substr(1);
                    }
                    
                    $("#user-numbers").append ("<tr><td>" + label + "</td><td>" + number + "</td></tr>");
                });
                
                if (looped) {
                    $("#user-numbers-label").removeClass ("hidden");
                    $("#user-numbers").removeClass ("hidden");
                }
            } else {
                $("#user-numbers-label").addClass ("hidden");
                $("#user-numbers").addClass ("hidden");
            }
        });
        
        // The buck stops here.
        return false;
    });
}); 
