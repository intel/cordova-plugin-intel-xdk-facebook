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

var exec = require('cordova/exec');

module.exports = {

	login: function(permissions) {
		if( typeof( permissions ) == "undefined" ) permissions = "publish_stream,publish_actions,offline_access";
		
		//AppMobiFacebook.login(permissions);
		exec(function(id) {
			}, null, "Facebook", "login", [permissions]);
	},
	logout: function() {
		exec(function(id) {
			}, null, "Facebook", "logout", []);
	},
	requestWithGraphAPI: function(path, method, parameters) {
		exec(function(id) {
			}, null, "Facebook", "requestWithGraphAPI", [path, method, parameters]);
	},
	requestWithRestAPI: function(path, method, parameters) {
		exec(function(id) {
			}, null, "Facebook", "requestWithRestAPI", [path, method, parameters]);
	},
	showAppRequestDialog: function(parameters) {
		exec(function(id) {
			}, null, "Facebook", "showAppRequestDialog", [parameters]);
	},
	showNewsFeedDialog: function(parameters) {
		exec(function(id) {
			}, null, "Facebook", "showNewsFeedDialog", [parameters]);
	},
	enableFrictionlessRequests: function(parameters) {
		exec(function(id) {
			}, null, "Facebook", "enableFrictionlessRequests", [parameters]);
	}
}