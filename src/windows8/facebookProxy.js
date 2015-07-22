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


// This try/catch is temporary to maintain backwards compatibility. Will be removed and changed to just 
// require('cordova/exec/proxy') at unknown date/time.
var commandProxy;
try {
    commandProxy = require('cordova/windows8/commandProxy');
} catch (e) {
    commandProxy = require('cordova/exec/proxy');
}

module.exports = {
    fbAppId: null,

    getFacebookInfo: function (successCallback, errorCallback, params) {
        var me = module.exports;

        window.FB = FBWinJS;

        var installFolder = Windows.ApplicationModel.Package.current.installedLocation;

        installFolder.getFolderAsync("properties").then(function (folder) {
            folder.getFoldersAsync().then(
                function (folders) {
                    folders.forEach(
                        function (folder) {
                            if (folder.name == "FBAppId") {
                                folder.getFoldersAsync().then(function (folder) {
                                    me.fbAppId = folder[0].name;
                                    FB.options({ 'appId': me.fbAppId });
                                });
                            }
                        }
                    );

                    successCallback();
                }
            );
        });
    },

	login: function(successCallback, errorCallback, params) {
	    var me = module.exports;

	    var redirectUri = 'https://www.facebook.com/connect/login_success.html',
            loginUrl = 'https://www.facebook.com/dialog/oauth'
                + '?response_type=token'
                + '&display=popup'
                + '&scope=' + encodeURIComponent('user_about_me,publish_stream,read_stream')
                + '&redirect_uri=' + encodeURIComponent(redirectUri)
                + '&client_id=' + FB.options('appId');

	    try {

	        Windows.Security.Authentication.Web.WebAuthenticationBroker.authenticateAsync(
                Windows.Security.Authentication.Web.WebAuthenticationBroker.default,
                new Windows.Foundation.Uri(loginUrl),
                new Windows.Foundation.Uri(redirectUri))
                .then(function success(result) {
                    if (result.responseStatus == 2) {
                        console.log('error: ' + result.responseerrordetail);
                        return;
                    }

                    var parser = document.createElement('a');
                    parser.href = result.responseData;

                    var qs = extractQuerystring(parser.hash.substr(1).split('&'));

                    if (qs.error) {
                        // most likely user clicked don't allow
                        console.log('error: ' + qs.error + ' : ' + qs.error_description);
                        return;
                    }

                    var js = "";
                    // we now have the access token,
                    if (qs.access_token) {
                        // set it as the default access token.
                        FB.setAccessToken(qs.access_token);

                        // save it in local storage so can access it later
                        localStorage.setItem('fb_access_token', FB.getAccessToken());

                        var e = document.createEvent('Events');
                        e.initEvent('intel.xdk.facebook.login', true, true);
                        e.success = true; e.token = '';
                        e.cancelled = false;
                        document.dispatchEvent(e);
                    } else {
                        var e = document.createEvent('Events');
                        e.initEvent('intel.xdk.facebook.login', true, true);
                        e.success = false; e.token = '';
                        e.cancelled = true;
                        document.dispatchEvent(e);
                    }


                }, function error(err) {
                    console.log('Error Number: ' + err.number);
                    console.log('Error Message: ' + err.message);
                });

	    } catch (e) {
	        // error launching web auth
	        console.log(e);
	    }
	},

	logout: function(successCallback, errorCallback, params) {
	    FB.setAccessToken('');
	    
	    var e = document.createEvent('Events');
	    e.initEvent('intel.xdk.facebook.logout', true, true);
	    e.success = true;
	    document.dispatchEvent(e);
	},

	requestWithGraphAPI: function (successCallback, errorCallback, params) {
	    var path = params[0];
	    var method = params[1];
	    var paramss = params[2];

	    FB.api(path, function (res) {
	        if (!res || res.error) {
	            var e = document.createEvent('Events');
	            e.initEvent('intel.xdk.facebook.request.response', true, true);
	            e.success = false;
	            e.error = res.error;
	            e.raw = '';
	            e.data = {};
	            document.dispatchEvent(e);
                
	            console.log(!res ? 'error occurred' : res.error);
	            return;
	        }
	        console.log(res.id);
	        console.log(res.name);

	        var e = document.createEvent('Events'); e.initEvent('intel.xdk.facebook.request.response', true, true);
	        e.success = true;
	        e.raw = JSON.stringify(res);
	        e.data = {};
	        try { e.data = JSON.parse(e.raw); } catch (ex) { } e.error = '';
	        document.dispatchEvent(e);

	    });
	},

	requestWithRestAPI: function(successCallback, errorCallback, params) {
	    var e = document.createEvent('Events');
	    e.initEvent('intel.xdk.facebook.request.response', true, true);
	    e.success = false;
	    e.error = 'Facebook REST APIs have been deprecated.';
	    e.raw = '';
	    e.data = {};
	    document.dispatchEvent(e);
	},

	showAppRequestDialog: function(successCallback, errorCallback, params) {
	    FB.api('me/apprequests', 'post', params[0], function (res) {
	        if (!res || res.error) {
	            var e = document.createEvent('Events');
	            e.initEvent('intel.xdk.facebook.dialog.complete', true, true);
	            e.canceled = 'true';
	            e.success = false;
	            e.raw = '';
	            e.data = {};
	            document.dispatchEvent(e);
	            return;
	        }
	        console.log('Post Id: ' + res.id);

	        var requestId = res.request;
	        if (requestId != null) {

	            var e = document.createEvent('Events');

	            try {
	                e.request = requestId;
	            } catch (e1) {
	                // TODO Auto-generated catch block
	                //e1.printStackTrace();
	            }

	            var to = res.to;

	            e.to = [];
	            if (res.to) {
	                for (var i = 0; i < to.length; i++) {
	                    e.to.push(to[i]);
	                }
	            }

	            e.initEvent('intel.xdk.facebook.dialog.complete', true, true);
	            e.success = true; e.error = '';
	            e.raw = JSON.stringify(res);
	            e.data = {}; try { e.data = JSON.parse(e.raw); } catch (ex) { }

	            document.dispatchEvent(e);

	            //resetFBStatus();
	        }
	    });
	},

	showNewsFeedDialog: function(successCallback, errorCallback, params) {
	    FB.api('me/feed', 'post', params[0], function (res) {
	        if (!res || res.error) {
	            var e = document.createEvent('Events');
	            e.initEvent('intel.xdk.facebook.dialog.complete', true, true);
	            e.canceled = 'true';
	            e.success = false;
	            e.raw = '';
	            e.data = {};
	            document.dispatchEvent(e);
	            return;
	        }
	        console.log('Post Id: ' + res.id);

	        var postId = res.id;
	        if (postId != null) {

	            var e = document.createEvent('Events');

	            try {
	                var request = res.ToString();
	                e.request = request;
	            } catch (e1) {
	                // TODO Auto-generated catch block
	                //e1.printStackTrace();
	            }

	            var to = res.to;

	            e.to = [];
	            if (res.to) {
	                for (var i = 0; i < to.Count; i++) {
	                    e.to.push(to[i]);
	                }
	            }

	            e.initEvent('intel.xdk.facebook.dialog.complete', true, true);
	            e.success = true; e.error = '';
	            e.raw = '" + result.ToString() + "';
	            e.data = {}; try { e.data = JSON.parse(e.raw); } catch (ex) { }

	            document.dispatchEvent(e);

	            //resetFBStatus();
	        }
	    });
	},

	enableFrictionlessRequests: function(successCallback, errorCallback, params) {

	}
}

commandProxy.add('Facebook', module.exports);

function extractQuerystring(a) {
    if (a == "") return {};
    var b = {};
    for (var i = 0; i < a.length; ++i) {
        var p = a[i].split('=');
        if (p.length != 2) continue;
        b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
    }
    return b;
}
