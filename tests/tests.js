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

/*global exports, describe, xdescribe, it, xit, expect, jasmine*/
/*global document, intel, console */

exports.defineAutoTests = function () {
    'use strict'; 
    
    /** Skipped test suite
     * intel.xdk.facebook is undefined
     */
    describe('API Tests', function () {
        it('should be defined', function () {
            expect(intel.xdk.facebook).toBeDefined();
        });
        
        it('should have a enableFrictionlessRequests method', function () {
            expect(intel.xdk.facebook.enableFrictionlessRequests).toBeDefined();
        });
        
        it('should have a login method', function () {
            expect(intel.xdk.facebook.login).toBeDefined();
        });
        
        it('should have a logout method', function () {
            expect(intel.xdk.facebook.logout).toBeDefined();
        });
        
        it('should have a requestWithGraphAPI method', function () {
            expect(intel.xdk.facebook.requestWithGraphAPI).toBeDefined();
        });
        
        it('should have a requestWithRestAPI method', function () {
            expect(intel.xdk.facebook.requestWithRestAPI).toBeDefined();
        });
    });
};

exports.defineManualTests = function (contentEl, createActionButton) {
    'use strict';
    
    function logMessage(message, color) {
        var log = document.getElementById('info'),
            logLine = document.createElement('div');
        
        if (color) {
            logLine.style.color = color;
        }
        
        logLine.innerHTML = message;
        log.appendChild(logLine);
    }

    function clearLog() {
        var log = document.getElementById('info');
        log.innerHTML = '';
    }
    
    function testNotImplemented(testName) {
        return function () {
            console.error(testName, 'test not implemented');
        };
    }
    
    function init() {
        
        document.addEventListener("intel.xdk.facebook.dialog.complete",function(e) {
            console.log('event:',e.type);
            if (e.success === true) {
                console.log('success');
            } else { 
                console.error('fail'); }
        },false);

        document.addEventListener("intel.xdk.facebook.request.response", function (e) {
            console.log('event:',e.type);
            if (e.success === true) {
                console.log('success:',e.data.data);
            } else {
                console.error('error:',e.error);
            }
        },false);

        document.addEventListener("intel.xdk.facebook.login",function(e){
            console.log('event:',e.type);
            console.log(e.success === true? "login success":"login fail"); 
        },false); 

        document.addEventListener("intel.xdk.facebook.logout",function(e){
            console.log('event:',e.type);
            console.log(e.success === true? "logout success":"logout fail");
        },false); 

        document.addEventListener("intel.xdk.facebook.busy",function(e){
            console.log('event:',e.type);
            console.log('facebook is busy');
        },false); 
    }
    
    /** object to hold properties and configs */
    var TestSuite = {};
  
    TestSuite.$markup = '' +
                
        '<h3>Login</h3>' +
        '<div id="buttonLogin"></div>' +
        'Expected result: should display login view for facebook account' +
    
        '<h3>Logout</h3>' +
        '<div id="buttonLogout"></div>' +
        'Expected result: should logout current user from the facebook app' +
    
        '<h3>Enable Frictionless Requests</h3>' +
        '<div id="buttonEnableFrictionlessRequest"></div>' +
        'Expected result: [TODO: add expected result]' +
    
        '<h3>Request With Graph API</h3>' +
        '<div id="buttonRequestWithGraphAPI"></div>' +
        'Expected result: [TODO: add expected result]' +
    
        '<h3>Request With Rest API</h3>' +
        '<div id="buttonRequestWithRestAPI"></div>' +
        'Expected result: [TODO: add expected result]' +
    
        '<h3>Show App Request Dialog</h3>' +
        '<div id="buttonShowAppRequestDialog"></div>' +
        'Expected result: [TODO: add expected result]' +
    
        '<h3>Show News Feed Dialog</h3>' +
        '<div id="buttonShowNewsFeedDialog"></div>' +
        'Expected result: [TODO: add expected result]' +
        
        '';
        
    contentEl.innerHTML = '<div id="info"></div>' + TestSuite.$markup;
    
    createActionButton('login()', function () {
        console.log('executing: intel.xdk.facebook.login');
        intel.xdk.facebook.login("publish_stream,publish_actions,offline_access,user_friends");
    }, 'buttonLogin');

    createActionButton('logout()', function () {
        console.log('executing: intel.xdk.facebook.logout');
        intel.xdk.facebook.logout();
    }, 'buttonLogout');
    
    createActionButton('enableFrictionlessRequests()',function () {
        console.log('executing: intel.xdk.facebook.enableFrictionlessRequests');
        intel.xdk.facebook.enableFrictionlessRequests();
    }, 'buttonEnableFrictionlessRequest');

    createActionButton('requestWithGraphAPI()', function () {
        console.log('executing: intel.xdk.facebook.requestWithGraphAPI');
        intel.xdk.facebook.requestWithGraphAPI("me/friends","GET",null);
    }, 'buttonRequestWithGraphAPI');

    createActionButton('requestWithRestAPI()', function () {
        console.log('executing: intel.xdk.facebook.requestWithRestAPI');
        intel.xdk.facebook.requestWithRestAPI("facebook.Users.getInfo", "GET", "");
    }, 'buttonRequestWithRestAPI');
    
    createActionButton('showAppRequestDialog()', function () {
        console.log('executing: intel.xdk.facebook.showAppRequestDialog');
        
        var objParameters = {"to":"appmobi.ryan.5","message":"My Awesome Message","title":"A title for this dialog would go here"};
        //var objParameters = {"to":"appmobi.tony","message":"My Awesome Message","title":"A title for this dialog would go here"};
        //var objParameters = {"message":"My Awesome Message","title":"A title for this dialog would go here"};

        intel.xdk.facebook.showAppRequestDialog(objParameters);
    }, 'buttonShowAppRequestDialog');

    createActionButton('showNewsFeedDialog', function () {
        console.log('executing: intel.xdk.facebook.showNewsFeedDialog');
        
        var objParameters = {
            "picture":"http://fbrell.com/f8.jpg", 
            "name":"Facebook Dialog",
            "caption":"This is my caption",
            "description":"Using Dialogs to interact with users.",
            "link":"http://xdk.intel.com",
            "message":"My test message!"
        };
        
        intel.xdk.facebook.showNewsFeedDialog(objParameters);
    }, 'buttonShowNewsFeedDialog');
    
    document.addEventListener('deviceready', init, false);
};