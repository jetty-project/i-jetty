$(document).ready(function() { 
    $(".userlink").click (function() {
        var uri = $(this).attr("href") + "?json=1";
        $.getJSON(uri, function(data) {
            console.log ("JSON data from server: ", data);
            
            // Set name and notes fields in view pane
            $("#userinfo").removeClass ("hidden");
            $("#user-name").text (data.summary.name);
            if (data.summary.notes) {
                $("#user-notes-label").removeClass ("hidden");
                $("#user-notes").removeClass ("hidden");
                $("#user-notes").text (data.summary.notes);
            } else {
                $("#user-notes-label").addClass ("hidden");
                $("#user-notes").addClass ("hidden");
                $("#user-notes").text ("");
            }
        });
        
        return false;
    });
}); 
