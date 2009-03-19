var User = 
{
        labels: 
        { 
            1 : "Home",
            2 : "Mobile",
            3 : "Work",
            4 : "Work Fax",
            5 : "Home Fax",
            6 : "Pager",
            7 : "Other",
            8 : "Custom..."
        },
        
        kinds:
        {
            1 : "Email",
            2 : "IM",
            3 : "Phone",
            4 : "Postal"
        },

        addresses: [],
        phones: {},
        summary : 
        {
            name: "",
            id: 0,
            notes: "",
            starred: false,
            voicemail: false
        },
        
        addUser: function ()
        {
            
        },

        deleteUser: function ()
        {
            var id = $('#user-id').attr ('value');
            $('#userinfo').hide ('slow');
            $('#user-' + id).remove ();    
            $.post ("/console/contacts/"+User.summary.id+"/?action=3&json=1", 
                    null, 
                    function(data) 
                    {
                        if (!data.status || !data.status == "OK")
                            console.log("Bad status on delete of user");
                    });

            return false;
        },

        starUser: function ()
        {
            $('#user-star').toggleClass ('starred');
        },

        addNumber: function ()
        {
            $('#user-numbers').removeClass ('hidden');
            var optionstr = "";
            jQuery.each(User.labels, 
                        function (value, label) 
                        {
                            optionstr += '<option value="' + value + '">' + label + '</option>';
                        });

            // If this is the second time 'add' was clicked, make the previous editable row
            // become static again, and attach in-place edit events.
            if ($('#user-last-number-row').length > 0) 
            {
                var val = $('#user-last-number-label').attr ('value');
                var num = $('#user-last-number').attr ('value');

                var labelcol = $($('#user-last-number-row').children()[0]);
                labelcol.empty();
                labelcol.append ('<span class="user-number-label-editable">' + labels[val] + '</span>');
                // FIXME: Merge with "selected" for edit-in-place widget.
                $('.user-number-label-editable:last').editable(submitFunction, {type: 'select', submit: 'OK', 'data': labels });

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
        },
        
        addAddress: function ()
        {
            
        },
        
        addNote: function () 
        {
            $('#user-notes').removeClass ('hidden');
            $('#user-notes').text(' ');
            $('#user-notes').trigger ('click');
            return false;
        },

        parseJson: function (data)
        {
            if (!data.summary) 
            {
                alert ('Server returned bad data.');
                return false;
            }
            
            User.summary = data.summary;

            if (data.phones) 
                User.parsePhones(data.phones);
            
            if (data.contacts)
                User.parseAddresses(data.contacts);
        },
        
        parsePhones: function (phoneData)
        {
            if (!phoneData) 
                return;

            jQuery.each(phoneData, 
                        function (number, info) 
                        {
                            User.phones[number] = info;
                        });
        },
        
        parseAddresses: function (addressData)
        {
            jQuery.each(addressData, 
                        function (info)
                        {
                            if (!info)
                                return;
                            User.addresses.push(info);
                        });
        },
        
        renderUser: function ()
        {
            $('#user-id').attr ('value', User.summary.id);
            $('#userinfo').show();
            $('#userinfo').removeClass ('hidden');
            $('#user-name').text (User.summary.name);

            $('#user-call').attr ('href', '/console/contacts/' + User.summary.id + '/?action=0');

            $('#user-photo').attr ('src', '/console/contacts/' + User.summary.id + '/photo');
            $('#user-photo-link').attr ('href', '/console/contacts/' + User.summary.id + '/photo');

            $('#user-star').removeClass('starred');

            if (User.summary.starred && User.summary.starred == true) 
            {
                $('#user-star').addClass('starred');
            }

            if (User.summary.notes) 
            {
                $('#user-notes').text (User.summary.notes);
            } 
            else 
            {
                $('#user-notes').text ('Click to add a note.');
            }
            $('#user-numbers').empty();

            User.renderPhones();
            User.renderAddresses();
            
            $('#user-numbers-add').click (User.addNumber);
            $('#user-notes-add').click (User.addNote);
            $('#user-delete').click (User.deleteUser);
            $('#user-star').click (User.starUser);
                  
            /*
            $('#user-numbers').addClass ('hidden');
            $('#user-contacts').addClass('hidden');
            */
            // If edit hint is visible, hide it. This only happens the first time the user clicks on a contact.
            // FIXME: Probably have cookie to handle this, so it only appears once, EVER!
            if ($('#user-edit-hint').css('display') != 'none') 
            {
                setTimeout("$('#user-edit-hint').hide('slow');", 5000);
            }
        },

        renderPhones: function ()
        {     
            for (var p in User.phones)
            {
                var row = "<tr><td>";
                row += '<span class="user-number-label-editable">' + (User.phones[p].label ? User.phones[p].label :  User.labels[User.phones[p].type])+ '</span></td>';
                row += '<td><a href="#" class="user-number-remove"><img src="/console/list-remove.png" alt="Remove" /></a><span class="user-number-editable">' + User.phones[p].number + '</span></td></tr>';
                    
                $('#user-numbers').append(row); 
            }
            
            $('.user-number-label-editable').editable(User.doSubmit, { type: 'select', submit: 'OK'});
            $('.user-number-editable').editable(User.doSubmit, { submit: 'OK' });
            $('#user-numbers').removeClass ('hidden');
        },
        
        renderAddresses: function ()
        {
            for (var a in User.addresses)
            {
                var kind = User.kinds[User.addresses[a].kind];
                var label = User.labels[User.addresses[a].type];
                var data = User.addresses[a].data;
                var aux = User.addresses[a].aux;
                
                var row = "<tr><td>";
                //
                //row += "<span class='contact-label-editable'>"+kind+"</span></td>";
                //row += "<td><span class='contact-label-editable'>"+label+"</span></td>";
                //row += "<td><span class='contact-editable'>"+data+"</span>"+(aux?"<span class='contact-editable'>"+aux+"</span>":"")+"</td></tr>");
                $('#user-contacts').append(row);
            }
            $('.contact-label-editable').editable(User.doSubmit, { type: 'select', submit: 'OK' });
            $('.contact-editable').editable(User.doSubmit, { submit: 'OK' });
            $('#user-contacts').removeClass ('hidden');
        },

        doSubmit: function (value, settings)
        {
           
            var myuser = 
            {
                "id": $('#user-id').attr ('value'),
                "name": $('#user-name').text (),
                "notes": $('#user-notes').text (),
                "starred": $('#user-star').hasClass ('starred'),
                "voicemail": true
            };
    
            // Make name in table match that just edited
            //$('#user-' + myuser.summary.id + ' a.userlink').text (myuser.summary.notes);

            // FIXME: Do phone numbers and addresses

            // Send data to server via POST
            $.post ("/console/contacts?action=4&json=1", 
                    myuser, 
                    function(data) 
                    {
                        console.log ("POST returned: ", data);
                        User.parseJson (data);
                        User.renderUser();
                    });

            return value;         
        },

        editUser: function ()
        {
            /*$('#user-notes').editable(User.doSubmit, { type: 'textarea', submit: 'OK' });
            $('#user-name').editable(User.doSubmit, { submit: 'OK' }); */
            var uri = $(this).attr('href') + "?json=1";
            console.log("uri = "+uri);
            $.ajax(
                   {
                       type: 'GET',
                       url: 'http://localhost:8888/console/contacts/2/?json=1',
                       dataType: 'json',
                       beforeSend: function(xhr)
                                   {
                                       xhr.setRequestHeader('Connection', 'Keep-Alive');
                                       return true;
                                    },
                       success: function(response) 
                                { 
                                        console.log(response);
                                    User.parseJson(response); 
                                    User.renderUser();
                                    User.renderPhones();
                                    User.renderAddresses();
                                },
                       error: function(xhr, reason, exception) 
                              { 
                                  console.log("Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                              }
                   });
            return false;
        }

};

    


$(document).ready (
                   function () 
                   {
                       var content = $("div id='content'>");
                       
                       var div = $("<div class='hidden' id='userinfo'>");
                       content.append(form);
                       
                       var form = $("<form onsubmit='return false;'>");
                       div.append(form);
                       
                       var input = $("<input type='hidden' id='user-id'>");
                       form.append(input);
                       
                       var hint = $("<small id='user-edit-hint'>");
                       hint.html("Click on some text to edit it");
                       input.after(hint);
                       
                       var h1 = $("<h1 style='margin-top: 2px; margin-bottom: 2px;' id='user-name'>");
                       hint.after(h1);

                       var small = $("<small>");
                       small.html("<div class='star' id='user-star'>*</div> | <a href='#' id='user-delete'>Delete</a> | <a href='#' id='user-call'>Call</a>");
                       h1.after(small);
                       
                       var pic = $("<a href='#' id='user-photo-link'>");
                       pic.html("<img id='user-photo' alt='User photo'/>");
                       small.after(pic);
                       
                       var h2 = $("<h2 id='user-notes-label'>");
                       h2.html("Notes");
                       pic.after(h2);
                      
                       var p = $("<p id='user-notes'>");
                       p.html("Click to add a note");
                       h2.after(p);
                       
                       var h2b = $("<h2 id='user-numbers-label'>");
                       h2b.html("<a href='#' id='user-numbers-add'><img src='/console/list-add.png' /></a>Phone Numbers");
                       p.after(h2b);
                       var tbl = $("<table id='user-numbers' class='hidden'>");
                       h2b.after(tbl);
                                
                       console.log("document ready");
                       $('#pg-header').after(content);
                       $('.userlink').click(User.editUser);
                   }); 

