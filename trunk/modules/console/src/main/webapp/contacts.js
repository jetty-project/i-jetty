var User = 
{
        action_none : -1,
        action_edit: 1,
        action_add: 2,
        action_del: 3,
        action_save: 4,
         
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
        
        types: 
        {
            0: "Custom",
            1: "Home",
            2: "Work",
            3: "Other"
        },
        
        mobileBrowser: false,
        addresses: [],
        phones: [],
        summary : 
        {
            name: "",
            id: 0,
            notes: "",
            starred: false,
            voicemail: false
        },
        users: [],
        version: -1,
        
        
        getUsers: function (successfn, errfn)
        {
            var uri =  "/console/contacts/dyn?json=1";
            console.log("uri = "+uri);
            $.ajax(
                   {
                       type: 'GET',
                       url: uri,
                       dataType: 'json',
                       beforeSend: function(xhr)
                       {
                           xhr.setRequestHeader('Connection', 'Keep-Alive');
                           return true;
                       },
                       success: function(response) 
                       { 
                           if (!successfn)
                           {
                               console.log("Got user list");
                               User.users = response.users;
                               User.version = response.version;
                               User.renderUsers();
                           }
                           else
                               successfn(response);
                       },
                       error: function(xhr, reason, exception) 
                       { 
                           if (!errfn)
                           {
                               console.log("Get Users Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                           }
                           else
                               errfn (xhr, reason, exception);
                       }
                   });
            return false; 
        },
        
        getUserDetails: function ()
        {
            var uri = $(this).attr('href') + "?json=1";
            console.log("uri = "+uri);
            $.ajax(
                   {
                       type: 'GET',
                       url: uri,
                       dataType: 'json',
                       beforeSend: function(xhr)
                       {
                           xhr.setRequestHeader('Connection', 'Keep-Alive');
                           return true;
                       },
                       success: function(response) 
                       { 
                           if (!response.summary) 
                           {
                               alert ('Server returned bad data.');
                               return;
                           }

                           User.summary = response.summary;

                           if (response.phones) 
                               User.phones = response.phones;

                           if (response.contacts)
                               User.addresses = response.contacts;

                           User.version = response.version;
                           User.renderUser();
                       },
                       error: function(xhr, reason, exception) 
                       { 
                           console.log("Get User Request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                       }
                   });
            return false;
        },
        
        refreshUsers: function ()
        {
            //Set a timeout as the async xhr request often completes before the form submission
            setTimeout("User.getUsers(User.checkResponseVersion, User.errResponseVersion)",
                       1000);
        },
        
        checkResponseVersion: function (response)
        {
            if (User.version == response.version)
                User.refreshUsers();
            else
            {
                User.version = response.version; 
                User.users = response.users; 
                User.renderUsers();
            }
        },
        
        errResponseVersion: function ()
        {
            console.log("Error getting updated user list");
        },
        
        addUser: function ()
        {
            User.summary.name = "";
            User.summary.id = "";
            User.summary.notes = "";
            User.summary.starred = false;
            User.summary.voicemail = false;
            User.phones = [];
            User.addresses = [];
            User.renderUser();
        },

        deleteUser: function ()
        {
            $.ajax (
                    {
                        type: 'POST',
                        url: "/console/contacts/dyn/"+User.summary.id+"/?action="+User.action_del+"&json=1",
                        dataType: 'json',
                        success: function (response)
                        {
                            if (response.status && response.status == "OK")
                            {
                               //remove the user from the list
                               for (var u in User.users)
                               {
                                   if (User.users[u].id == User.summary.id)
                                   {
                                       delete User.users[u];
                                       break;
                                   }
                               }

                               if (User.mobileBrowser)
                               {
                                   $("#content").html(""); //replace the content div with the list of users
                                   User.renderUsers();
                               }
                               else
                               {  
                                   //get rid of the userinfo div   
                                   $("#contacts").html("");
                                   $("#userinfo").remove();
                                   User.renderUsers();
                               }
                            }
                            else
                               console.log(data);
                        },
                        error: function(xhr, reason, exception) 
                        { 
                            console.log("Delete request failed, status: "+(xhr && xhr.status)+" reason: "+reason+" exception: "+ exception); 
                        }
                    });
        },
     
        
        saveUser: function ()
        {     
           User.refreshUsers();
        },
        
     
      
        
        cancelEdit: function ()
        {
            //Remove the editable user representation
            if (User.mobileBrowser)
            {
                $("#content").html(""); //replace the content div with the list of users
                User.renderUsers();
            }
            else
            {  
                console.log("Not a mobile browser, removing the userinfo div");
                //get rid of the userinfo div
                $("#userinfo").remove();
            }
            return false;
        },
        
        renderUser: function ()
        {            
            $("#userinfo").remove();
            var html = "<div id='userinfo'>";
            html += "<form id='uform' method='POST' action='/console/contacts/dyn/?action="+User.action_save+"&json=1' enctype='multipart/form-data' target='hidden-target' onSubmit='User.saveUser(); return true;'>";
            html += "<input type='hidden' name='id' value='" + User.summary.id + "'>";
            html += "<table><tr><td colspan='3'><h2>General</h2></td></tr>";
            html += "<tr><td>Name: </td><td colspan='2' ><input name='name' type='text' value='" + User.summary.name + "' /></td></tr>";
            html += "<tr><td>Starred: </td><td colspan='2' ><input name='starred' type='checkbox' " + (User.summary.starred ? "checked='checked'" : "") + " /></td></tr>";
            html += "<tr><td>Send to Voicemail: </td><td colspan='2'><input name='voicemail' type='checkbox' "+(User.summary.voicemail? "checked='checked'" : "") + " /><td></tr>";
            html += "<tr><td>Notes: </td><td colspan='2'  ><textarea name='notes'>" + (User.summary.notes != null ? User.summary.notes : "") + "</textarea></td></tr>";
            html += "<tr><td colspan='2'><a href='/console/contacts/dyn/"+User.summary.id+"/photo'><img src='/console/contacts/dyn/"+User.summary.id+"/photo' /></a></td><td><input type='file' name='new-pic'>Change photo</input></td>";
            html += "<tr><td colspan='3'><h2>Phone numbers</h2></td></tr>";
            for (var p in User.phones)
            {
                html += User.renderPhone(User.phones[p]);
            }
            //add an extra blank for adding new number
            html += User.renderPhone ({number: "", id: "x", label: "", type: ""});
            html += "<tr><td colspan='3'><h2>Addresses</h2></td></tr>";
            for (var a in User.addresses)
            {
                html += User.renderAddress(User.addresses[a]);
            }
            //add an extra blank for adding a new address
            html += User.renderAddress({ id: "x", data: "", aux: "",  label: "", primary: false, kind: "", type:  ""});
            html += "</table>";
         
            html += "<br /><input type='submit' id='save' value='Save'/> ";  
            
            if (User.summary.id)
                html += "<button id='del' onclick='User.deleteUser();'>Delete</button>";
            html += "<button id='cancel' onclick='User.cancelEdit();'>Cancel</button>";
            html += "</form>";
            var content = $("#content");
            //if a mobile browser, we want to change the div content,
            //otherwise we want to add the div for the user
            if (User.mobileBrowser)
            {
                content.html("");
                content.append(html);
            }
            else
                content.append(html); 
        },

        renderPhone: function (phone)
        {
            var html = "<tr>"; 
            if (phone.id != "x")
                html += "<td><input type='checkbox' name='phone-del-"+phone.id+"' value='del'>Delete</input></td>";  
            else
                html += "<td>&nbsp;</td>";
            
            var selected = " selected='selected'";
            html += "<td><select name='phone-type-" + phone.id + "'>";
            for (var l in User.labels)
            {     
                html += "<option value='" + l + "'" + (phone.type == l ? selected : "") + ">" + User.labels[l] + "</option>";
            }   
            html += "</select></td>";     
            html += "<td><input type='text' name='phone-number-" + phone.id + "' id='phone-number-" + phone.id + "' style='width: 120px;' length='12' value='" + phone.number + "' /></td></tr>";
            return html;
        },
        

        renderAddress: function (address)
        {
            var html = "<tr>";
            if ("x" != address.id)
                html += "<td><input type='checkbox' name='contact-del-"+address.id+"' value='del'>Delete</input></td>";
            else
                html += "<td>&nbsp;</td>";
            
            var kindSelect = "<select name='contact-kind-"+address.id+"'>";
            for (var k in User.kinds)
            {
                kindSelect += "<option value='"+k+"'"+(address.kind == k? " selected='selected'": "")+">"+User.kinds[k]+"</option>";
            }
            kindSelect += "</select>";
            
            var typeSelect = "<select name='contact-type-"+address.id+"'>";
            for (t in User.types)
            {
                typeSelect += "<option value='"+t+"'"+(address.type == t? " selected='selected'": "")+">"+User.types[t]+"</option>";
            }
            typeSelect += "</select>";
        
            html +="<td>"+kindSelect+typeSelect+"</td><td><input type='text' name='contact-val-"+address.id+"' style='width: 120px;' length='12' value='"+address.data+"'/></td>";
            html +="</tr>";
            return html;
        },
       
        
        
        renderUsers: function ()
        {
            $("#userinfo").remove();
            $("#contacts").html("");
            var data = User.users;
            $("#pg-head").html("Contact List");
            $("#contacts").append("<table id='user'><thead><tr><th>Starred</th><th>Photo</th><th>Name</th></tr></thead><tbody id='ulist'></tbody></table>");
            var rows = "";
            for (var d in data)
            {
                rows += "<tr id='contact-" + data[d].id + "'>";
                if (data[d].starred)
                    rows += "<td><span class='big'>*</span></td>";
                else
                    rows += "<td>&nbsp;</td>";
                rows += "<td><a class='userlink' href='/console/contacts/dyn/"+data[d].id+"'><img src='/console/contacts/dyn/"+data[d].id+"/photo'/></a></td>";
                rows += "<td><a class='userlink' href='/console/contacts/dyn/"+data[d].id+"'>"+data[d].name+"</a></td>";
                rows +="</tr>";
            }
            $("#ulist").append(rows);
            $("#contacts").append("<button id='add' onclick='User.addUser();'>Add</button>");
            $('.userlink').click(User.getUserDetails);
        }
  
};

    


$(document).ready (
                   function () 
                   {
                       var agent = navigator.userAgent;
                       if (agent.indexOf("Android") > 0)
                           User.mobileBrowser = true;
 
                       User.getUsers();
                       console.log("document ready");
                   }); 

