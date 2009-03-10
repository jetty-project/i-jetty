var User = {
    addresses: [ ],
    phones: { },
    summary : {
        name: "",
        id: 0,
        notes: "",
        starred: false,
    },
};

$(document).ready (function () {
    var labels = { 
    		1 : "Home",
    		2 : "Mobile",
    		3 : "Work",
    		4 : "Work Fax",
    		5 : "Home Fax",
    		6 : "Pager",
    		7 : "Other",
    		8 : "Custom..."
    };
    
    var submitFunction = function (value, settings) {
        // Quick way to clone the User object
        // (since jQuery is lame and doesn't like non-Prototype classes)
        var myuser = $.extend({}, User);
        
        myuser.summary.id = $('#user-id').attr ('value');
        myuser.summary.name = $('#user-name').text ();
        myuser.summary.notes = $('#user-notes').text ();
        myuser.summary.starred = $('#user-star').hasClass ('starred');
        
        // Make name in table match that just edited
        $('#user-' + myuser.summary.id + ' a.userlink').text (myuser.summary.notes);
        
        // FIXME: Do phone numbers
        
        console.log ("This is: ", this);
        console.log ("myuser is: ", myuser);
        
        // Send data to server via POST        
        $.post ("/console/contacts?action=4&json=1", myuser, function(data) {
            console.log ("POST returned: ", data);
        });
        
        return value;
    };
    
    $('.userlink').click (function() {
        console.log ("This is: ", this);
        
        $('#user-notes').editable(submitFunction, { type: 'textarea', submit: 'OK' });
        $('#user-name').editable(submitFunction, { submit: 'OK' });
        
        var uri = $(this).attr('href') + '?json=1';
        $.getJSON(uri, function(data) {
            console.log ('JSON data from server: ', data);
            
            if (!data.summary) {
                alert ('Uh oh. Server returned bad data.');
                return;
            }
            
            $('#user-id').attr ('value', data.summary.id);
            
            $('#userinfo').show();
            $('#userinfo').removeClass ('hidden');
            $('#user-name').text (data.summary.name);
            
            $('#user-call').attr ('href', '/console/contacts/' + data.summary.id + '/?action=0');
            
            $('#user-photo').attr ('src', '/console/contacts/' + data.summary.id + '/photo');
            $('#user-photo-link').attr ('href', '/console/contacts/' + data.summary.id + '/photo');
            
            $('#user-star').removeClass('starred');
            
            if (data.summary.starred && data.summary.starred == true) {
                $('#user-star').addClass('starred');
            }
            
            if (data.summary.notes) {
                $('#user-notes').text (data.summary.notes);
            } else {
                $('#user-notes').text ('Click to add a note.');
            }
            
            $('#user-numbers').empty();
            
            if (data.phones) {
                var looped = false;
                jQuery.each(data.phones, function (number, info) {
                    looped = true;
                    
                    var label = 'Unknown';
                    if (info.label)
                        label = info.label;
                    
                    // FIXME: Make this properly editable.
                    $('#user-numbers').append ('<tr><td><span class="user-number-label-editable">' + label + '</span></td><td><a href="#" class="user-number-remove"><img src="/console/list-remove.png" alt="Remove" /></a><span class="user-number-editable">' + number + '</span></td></tr>');
                });
                
                if (looped) {
                    $('.user-number-label-editable').editable(submitFunction, { type: 'select', submit: 'OK', 'data': labels });
                    $('.user-number-editable').editable(submitFunction, { submit: 'OK' });
                    $('#user-numbers').removeClass ('hidden');
                }
            } else {
                $('#user-numbers').addClass ('hidden');
            }
        });
        
        // If edit hint is visible, hide it. This only happens the first time the user clicks on a contact.
        // FIXME: Probably have cookie to handle this, so it only appears once, EVER!
        if ($('#user-edit-hint').css('display') != 'none') {
            setTimeout("$('#user-edit-hint').hide('slow');", 5000);
        }
        
        // The buck stops here.
        return false;
    });
    
    $('#user-numbers-add').click (function () {
        $('#user-numbers').removeClass ('hidden');
        var optionstr = "";
        jQuery.each(labels, function (value, label) {
            optionstr += '<option value="' + value + '">' + label + '</option>';
        });
        
        // If this is the second time 'add' was clicked, make the previous editable row
        // become static again, and attach in-place edit events.
        if ($('#user-last-number-row').length > 0) {
            var val = $('#user-last-number-label').attr ('value');
            var num = $('#user-last-number').attr ('value');
            
            var labelcol = $($('#user-last-number-row').children()[0]);
            labelcol.empty();
            labelcol.append ('<span class="user-number-label-editable">' + labels[val] + '</span>');
            // FIXME: Merge with "selected" for edit-in-place widget.
            $('.user-number-label-editable:last').editable(submitFunction, { type: 'select', submit: 'OK', 'data': labels });
            
            var valcol = $($('#user-last-number-row').children()[1]);
            valcol.empty();
            valcol.append ('<a href="#" class="user-number-remove"><img src="/console/list-remove.png" alt="Remove" /></a><span class="user-number-editable">' + num + '</span>');
            $('.user-number-editable:last').editable(submitFunction, { submit: 'OK' });
            
            // Reset IDs.
            $('#user-last-number-row').attr('id', '');
            $('#user-last-number-label').attr('id', '');
            $('#user-last-number').attr('id', '');
        }
        
        $('#user-numbers').append ('<tr id="user-last-number-row"><td><select id="user-last-number-label">' + optionstr + '</select></td><td><input type="text" length="10" style="width: 40px;" id="user-last-number"></input></td></tr>');
        return false;
    });
    
    $('#user-notes-add').click (function () {
        $('#user-notes').removeClass ('hidden');
        $('#user-notes').text(' ');
        
        $('#user-notes').trigger ('click');
        return false;
    });
    
    $('#user-delete').click (function () {
        var id = $('#user-id').attr ('value');
        
        $('#userinfo').hide ('slow');
        $('#user-' + id).remove ();
        
        // FIXME: Send 'delete' request 
        return false;
    });
    
    $('#user-star').click (function () {
        $('#user-star').toggleClass ('starred');
    });
}); 
