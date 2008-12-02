$(document).ready(function() { 
    $(".userlink").click (function() {
        var uri = $(this).attr("href") + "?json=1";
        $.getJSON(uri, function(data) {
            console.log ("JSON data from server: ", data);
        });
        
        return false;
    });
}); 
