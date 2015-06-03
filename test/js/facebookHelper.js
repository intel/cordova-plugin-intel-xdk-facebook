/*
Copyright 2015 Intel Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file 
except in compliance with the License. You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the 
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific language governing permissions 
and limitations under the License
*/

if (!window.alert)
    window.alert = function (message) {
        var messageDialog = new Windows.UI.Popups.MessageDialog(message);
        messageDialog.showAsync();

    }

function login() {
    intel.xdk.facebook.login("publish_stream,publish_actions,offline_access,user_friends");      
}

function logout() {
    intel.xdk.facebook.logout();
}

function requestWithGraphAPI() {
    intel.xdk.facebook.requestWithGraphAPI("me/friends","GET",null);  
}

function requestWithRestAPI() {
    intel.xdk.facebook.requestWithRestAPI("facebook.Users.getInfo", "GET", "");
}

function showAppRequestDialog() {
    var objParameters = {"to":"appmobi.ryan.5","message":"My Awesome Message","title":"A title for this dialog would go here"}
    //var objParameters = {"to":"appmobi.tony","message":"My Awesome Message","title":"A title for this dialog would go here"}
    //var objParameters = {"message":"My Awesome Message","title":"A title for this dialog would go here"}
    
    intel.xdk.facebook.showAppRequestDialog(objParameters);
}

function enableFrictionlessRequests() {
    intel.xdk.facebook.enableFrictionlessRequests();
}


function showNewsFeedDialog() {
    var objParameters = {
            "picture":"http://fbrell.com/f8.jpg", 
            "name":"Facebook Dialog",
            "caption":"This is my caption",
            "description":"Using Dialogs to interact with users.",
            "link":"http://xdk.intel.com",
            "message":"My test message!"
    }
    intel.xdk.facebook.showNewsFeedDialog(objParameters);
}


/* ******************** Event Listeners ********************** */
document.addEventListener("intel.xdk.facebook.dialog.complete",function(e) {
        alert("Permissions Request Returned");
        if (e.success == true) {
                alert("News feed updated successfully");
        } else { alert("permissions request failed"); }
},false);

//var facebookUserID = "me";  //me = the user currently logged into Facebook
document.addEventListener("intel.xdk.facebook.request.response", function (e) {
        if (e.success == true) {
            alert("Facebook User Friends Data Returned");
                var data = e.data.data;
                var outHTML = "";

                for (var r=0; r< data.length; r++) {
                outHTML += "<img src='http://graph.facebook.com/" + data[r]["id"] + "/picture' info='" + data[r]["name"] + "' />";
                        
                }

                if (typeof(WinJS) != "undefined") {
                    WinJS.Utilities.setInnerHTMLUnsafe(document.getElementsByTagName("body")[0], outHTML);
                } else {
                    document.getElementsByTagName("body")[0].innerHTML = outHTML;
                }

        } else {
            alert(e.error);
        }
},false);


document.addEventListener("intel.xdk.facebook.login",function(e){
        if (e.success == true) 
        {
            alert('login success'); 
        } 
        else 
        {
            alert('login failed'); 
        }
},false); 

document.addEventListener("intel.xdk.facebook.logout",function(e){
        if (e.success == true) 
        { 
            alert("Logged out of Facebook"); 
        } 
        else 
        { 
            alert("Unsuccessful Logout"); 
        }
},false); 

document.addEventListener("intel.xdk.facebook.busy",function(e){
        alert('Facebook is busy');
},false); 
/* ******************** Event Listeners ********************** */


