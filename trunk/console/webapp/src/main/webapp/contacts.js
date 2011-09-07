var Contacts = 
{
		page_size : 10,
		page_pos : 0,
		total : 0, 
        action_none : -1,
        action_edit: 1,
        action_add: 2,
        action_del: 3,
        action_save: 4,
         
        labels: 
        { 
        	0 : "Custom",
            1 : "Home",
            2 : "Mobile",
            3 : "Work",
            4 : "Work Fax",
            5 : "Home Fax",
            6 : "Pager",
            7 : "Other"
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
        contacts: [],
        version: -1,
        
        dummy: function ()
        {
        	
        },
        
        
        prev:  function ()
        {
        	Contacts.page_pos = Contacts.page_pos - Contacts.page_size;
        	Contacts.getContacts();
        },
        
        
        next:  function ()
        {
        	Contacts.page_pos = Contacts.page_pos + Contacts.page_size;
        	Contacts.getContacts();
        },
        
        getIdByName: function (name)
        {
        	if (!Contacts.contacts)
        		return -1;
        	
        	var i;
        	var id = -1;
        	for (i=0;i<Contacts.contacts.length;i++)
        	{
        		if (Contacts.contacts[i].name.equals(name))
        			id = Contacts.contacts[i].id;
        	}
        	return id;
        },
        
        getContacts: function (successfn, errfn)
        {
            var uri =  "/console/rest/contacts/?pgStart="+Contacts.page_pos+"&pgSize="+Contacts.page_size;
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
                        Contacts.contacts = response.contacts;
                        Contacts.version = response.version;
                        Contacts.total = response.total;
                        Contacts.renderContacts();
                        
                    }
                    else
                        successfn(response);
                },
                error: function(xhr, reason, exception) 
                {             	
                    Contacts.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try fully reloading the page.");
                }
            });
            return false; 
        },
        
        getContactDetails: function (id)
        {
            var uri = "/console/rest/contacts/"+id;
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
                	if (response.error)
                    {
                	    alert(response.error+" contact id="+id);
                	    return;
                    }
                
                    Contacts.summary = response.summary;

                    if (response.phones) 
                        Contacts.phones = response.phones;

                    if (response.contacts)
                        Contacts.addresses = response.contacts;

                    Contacts.version = response.version;
                    Contacts.renderContact();
                },
                error: function(xhr, reason, exception) 
                {   
                    Contacts.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try fully reloading the page.");
                }
            });
            return false;
        },
        
        refreshContacts: function ()
        {
            //Set a timeout as the async xhr request often completes before the form submission
            setTimeout("Contacts.getContacts(Contacts.checkResponseVersion, Contacts.errResponseVersion)",
                       1000);
        },
        
        checkResponseVersion: function (response)
        {
            if (Contacts.version == response.version)
            {
                Contacts.refreshContacts();
            }
            else
            {
            	Contacts.total = response.total;
                Contacts.version = response.version; 
                Contacts.contacts = response.contacts; 
                Contacts.renderContacts();
            }
        },
        
        detectEnvironment: function ()
        {
            var agent = navigator.userAgent;
            if (agent.indexOf("Android") > 0) {
                Contacts.mobileBrowser = true;
            } else if (agent.indexOf("iPhone") > 0) {
                Contacts.mobileBrowser = true;
            }
            
            if (Contacts.mobileBrowser)
            {
                $("#userinfo").attr("style", "float: none;");
            }
            else if ($("#userinfo").attr("style") != null)
            {
                $("#userinfo").removeAttr("style");
            }
        },
        
        errResponseVersion: function ()
        {
            alert("Error getting updated Contacts list");
        },
        
        addContact: function ()
        {
            Contacts.summary.name = "";
            Contacts.summary.id = "";
            Contacts.summary.notes = "";
            Contacts.summary.starred = false;
            Contacts.summary.voicemail = false;
            Contacts.phones = [];
            Contacts.addresses = [];
            Contacts.renderContact();
        },

        deleteContact: function ()
        {
            $.ajax (
                    {
                        type: 'POST',
                        url: "/console/rest/contacts/"+Contacts.summary.id+"/?action="+Contacts.action_del,
                        dataType: 'json',
                        success: function (response)
                        {
                            if (response.status && response.status == "OK")
                            {
                               //remove the contact from the list
                               for (var u in Contacts.contacts)
                               {
                                   if (Contacts.contacts[u].id == Contacts.summary.id)
                                   {
                                       delete Contacts.contacts[u];
                                       break;
                                   }
                               }

                               $("#userinfo").html(""); //get rid of the user info
                               //replace page with the list of contacts
                               Contacts.renderContacts();
                            }
                            else
                               alert(data);
                        },
                        error: function(xhr, reason, exception) 
                        { 
                            Contacts.renderError("Sorry but the request failed. Perhaps you restarted i-jetty or there was a temporary network problem. Please try fully reloading the page.");
                        }
                    });
        },
     
        
        saveContact: function ()
        {     
           Contacts.refreshContacts();
        },
        
        sendEmail: function (addr)
        {
        	window.location.replace("ma"+"i"+"lto:"+addr);
        },
      
        
        cancelEdit: function ()
        {
            //Remove the editable contact representation
        	$("#userinfo").html("");
        	
        	//If mobile browser, recreate the contacts page
            if (Contacts.mobileBrowser)
            {
                Contacts.renderContacts();
            }
            
            return false;
        },
        
        renderContact: function ()
        {     
        	if (Contacts.mobileBrowser)
        	{
        		$("#contacts").html(""); //only show contacts OR user details not both
        	}
        	
        	$("#userinfo").html(""); //get rid of any previous
            var html = "";
            html += "<form id='uform' method='POST' action='/console/rest/contacts/?action="+Contacts.action_save+"' enctype='multipart/form-data' target='hidden-target' onSubmit='Contacts.saveContact(); return true;'>";
            html += "<input type='hidden' name='id' value='" + Contacts.summary.id + "'>";
            html += "<table><tr><td colspan='2'><h2>General</h2></td></tr>";
            html += "<tr><td>Name: </td><td><input name='name' type='text' value='" + Contacts.summary.name + "' /></td></tr>";
            html += "<tr><td>Starred: </td><td ><input name='starred' type='checkbox' " + (Contacts.summary.starred ? "checked='checked'" : "") + " /></td></tr>";
            html += "<tr><td>Send to Voicemail: </td><td><input name='voicemail' type='checkbox' "+(Contacts.summary.voicemail? "checked='checked'" : "") + " /></td></tr>";
            html += "<tr><td>Notes: </td><td ><textarea name='notes'>" + (Contacts.summary.notes != null ? Contacts.summary.notes : "") + "</textarea></td></tr>";
            html += "<tr><td><a href='/console/rest/contacts/photo/"+Contacts.summary.id+"'><img src='/console/rest/contacts/photo/"+Contacts.summary.id+"' width='64' height='64'/></a></td><td>Update photo:<br /><input type='file' name='new-pic' /></td></tr>";
            html += "<tr><td colspan='2'><h2>Phone numbers</h2></td></tr>";
            for (var p in Contacts.phones)
            {
                html += Contacts.renderPhone(Contacts.phones[p]);
            }
            //add an extra blank for adding new number
            html += Contacts.renderPhone ({number: "", id: "x", label: "", type: "0"});
            html += "<tr><td colspan='2'><h2>Addresses</h2></td></tr>";
            for (var a in Contacts.addresses)
            {
                html += Contacts.renderAddress(Contacts.addresses[a]);
            }
            //add an extra blank for adding a new address
            html += Contacts.renderAddress({ id: "x", data: "", aux: "",  label: "", primary: false, kind: "", type:  "0"});
            html += "</table>";
         
            html += "<br /><input type='submit' id='save' value='Save'/> ";  
            
            if (Contacts.summary.id)
                html += "<button id='del' onclick='Contacts.deleteContact(); return false;'>Delete</button>&nbsp;";
            html += "<button id='cancel' onclick='Contacts.cancelEdit(); return false;'>Cancel</button>&nbsp;";
            html += "</form>";
         
            $("#userinfo").html(html); 
        },
        
        
        toggleLabel: function (entity, option, id)
        {
        	//entity is what has changed: phone or contact
        	//option is the select option
            //id is which has been changed
            //If option 0 is selected, this is type[0], which is Custom then
            //make the hidden text box visible
        	var selector = "#"+entity+"-type-"+id;
        	var sel = $(selector)[0].selectedIndex;
            if (sel == option)
                $("#"+entity+"-type-label-"+id).css("visibility", "visible");
            else
                $("#"+entity+"-type-label-"+id).css("visibility", "hidden");
        },
        
        renderPhone: function (phone)
        {
            var html = "<tr><td>"; 
            var selected = " selected='selected'";
            html += "<select id='phone-type-"+phone.id+"' name='phone-type-" + phone.id + "' onChange='Contacts.toggleLabel(\"phone\", 0, \""+phone.id+"\")'>";
            for (var l in Contacts.labels)
            {     
                html += "<option value='" + l + "'" + (phone.type == l ? selected : "") +">" + Contacts.labels[l] + "</option>";
            }   
            html += "</select>";
            html += "<input type='txt' id='phone-type-label-"+phone.id+"' name='phone-type-label-"+phone.id+"' value='"+phone.label+"' style='visibility: "+(phone.type=="0"?"visible":"hidden")+";' length='12'/>";
            
            html += "</td>";     
            html += "<td><input type='text' name='phone-number-" + phone.id + "' id='phone-number-" + phone.id + "' style='width: 120px;' length='12' value='" + phone.number + "' />";
            if (phone.id != "x")
                html += "&nbsp;<input type='checkbox' name='phone-del-"+phone.id+"' value='del'>Delete</input>";  
            
            html += "</td></tr>";
            return html;
        },
        

      
        renderAddress: function (address)
        {
            var html = "<tr><td>";
            
            var kindSelect = "<select name='contact-kind-"+address.id+"'>";
            for (var k in Contacts.kinds)
            {
                kindSelect += "<option value='"+k+"'"+(address.kind == k? " selected='selected'": "")+">"+Contacts.kinds[k]+"</option>";
            }
            kindSelect += "</select>";
            
            var typeSelect = "<select id='contact-type-"+address.id+"' onChange='Contacts.toggleLabel(\"contact\", 0, \""+address.id+"\")'  name='contact-type-"+address.id+"'>";
            for (t in Contacts.types)
            {
                typeSelect += "<option value='"+t+"'"+(address.type == t? " selected='selected'": "")+">"+Contacts.types[t]+"</option>";
            }
            typeSelect += "</select>";
            
            var typeLabel = "<input type='txt' id='contact-type-label-"+address.id+"' name='contact-type-label-"+address.id+"' value='"+address.label+"' style='visibility: "+(address.type=="0"?"visible":"hidden")+";' length='12'/>";

            html +=kindSelect+typeSelect+typeLabel+"</td><td><input type='text' name='contact-val-"+address.id+"' style='width: 120px;' length='12' value='"+address.data+"'/>";
            if ("x" != address.id)
            {
            	html += "&nbsp;&nbsp;<input type='checkbox' name='contact-del-"+address.id+"' value='del'>Delete</input>";     
            	if (address.kind == 1)
            		html += "&nbsp;&nbsp;<button onClick='Contacts.sendEmail(\""+address.data+"\"); return false;'>Send Email</button>";
            	
            }
            
            html +="</td></tr>";
            return html;
        },
       
        
        
        renderContacts: function ()
        {
        	$("#userinfo").html(""); //get rid of any previous user details
        	$("#contacts").html(""); //get rid of any previous contacts
    
            var data = Contacts.contacts;
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
                rows += "<td><a class='userlink'  onClick='Contacts.getContactDetails(\""+data[d].id+"\");'><img width='64' height='64' src='/console/rest/contacts/photo/"+data[d].id+"'/></a></td>";
                rows += "<td><a class='userlink'  onClick='Contacts.getContactDetails(\""+data[d].id+"\");'>"+data[d].name+"</a></td>";
                rows +="</tr>";
            }
            $("#ulist").append(rows);
            $("#contacts").append("<br /><button id='add' onclick='Contacts.addContact();'>Add Contact</button>");
            
            
            if (Contacts.page_pos > 0 && Contacts.contacts.length > 0)
                $("#contacts").append("<button id='prev' onclick='Contacts.prev();'>Prev Page</button>");
  
            
            if (Contacts.total > (Contacts.page_pos + Contacts.page_size))
             $("#contacts").append("<button id='next' onclick='Contacts.next();'>Next Page</button>");
            
            //$('.userlink').click(Contacts.getContactDetails);
            
            // make it sortable
            $("#user").tablesorter({sortList: [[2,0]],  headers: { 0: { sorter: false}, 1: {sorter: false}}});
        },
        
        renderError: function (msg)
        {
            $("#userinfo").html(""); //get rid of any previous user details
            $("#contacts").html(""); //get rid of any previous contacts
            $("#userinfo").html("<p class='error'>"+msg+"</p>");
        }
  
};




$(document).ready (function () {
    Contacts.detectEnvironment();
    Contacts.getContacts();
    var idx = document.URL.indexOf("?id=");
    if (idx >=0)
    {
    	var id = document.URL.substring(idx+4);
    	Contacts.getContactDetails(id);
    }
    	

}); 
