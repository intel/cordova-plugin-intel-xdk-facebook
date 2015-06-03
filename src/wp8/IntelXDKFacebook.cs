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

using Facebook;
using Facebook.Client;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Navigation;
using System.Windows.Threading;
using Windows.Storage;
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;

namespace Cordova.Extension.Commands
{
    public class Facebook : BaseCommand
    {
        private FacebookSessionClient facebookSessionClient;
        private FacebookSession session;
        private FacebookClient facebookClient;

        private string appId = string.Empty;
        private string accessToken = String.Empty;
        private string facebookId = String.Empty;

        private bool busy = false;
        public int currentCommand = 0;
        public String[] currentCommandArguments;

        #region Constructor
        public Facebook()
        {
            GetInfo();
        }
        #endregion

        #region Public Methods
        public void login(string parameters)
        {
            string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);

            LoginFacebook();
        }

        public void logout(string parameters)
        {
            string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);
            facebookSessionClient.Logout();
            string js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.logout',true,true);e.success=true;document.dispatchEvent(e);";
            InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
        }

        public void requestWithGraphAPI(string parameters)
        {
            RequestWithGraphAPI(parameters);
        }

        public void requestWithRestAPI(string parameters)
        {
            string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);

            string js = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='Facebook REST APIs have been deprecated.';e.raw='';e.data={};document.dispatchEvent(e);";
            InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
        }

        public void showAppRequestDialog(string parameters)
        {
            ShowAppRequestDialog(parameters);
        }

        public void showNewsFeedDialog(string parameters)
        {
            ShowNewsFeedDialog(parameters);
        }
        
        public void enableFrictionlessRequests(string parameters)
        {
            string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);

            string js = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='Facebook Frictionless is not available in Windows 8.';e.raw='';e.data={};document.dispatchEvent(e);";
            InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
        }
        #endregion

        #region Private Methods
        private async Task GetInfo()
        {
            StorageFolder FBFolder = await StorageFolder.GetFolderFromPathAsync(Path.Combine(Windows.ApplicationModel.Package.Current.InstalledLocation.Path, "props", "FBAPPID"));

            IReadOnlyList<StorageFolder> folders = await FBFolder.GetFoldersAsync();

            foreach (var folder in folders)
            {
                appId = folder.Name;
            }

            facebookSessionClient = new FacebookSessionClient(appId);
        }

        private async Task LoginFacebook()
        {
            Deployment.Current.Dispatcher.BeginInvoke(async() =>
            {
                try
                {
                    session = await facebookSessionClient.LoginAsync("user_about_me,read_stream");

                    string js = "";

                    if (session != null)
                    {
                        accessToken = session.AccessToken;
                        facebookId = session.FacebookId;
                        facebookClient = new FacebookClient(accessToken);

                        js = String.Format("var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.login',true,true);e.success=true;e.token='';e.cancelled={0};document.dispatchEvent(e);", "false");
                    }
                    else
                    {
                        js = String.Format("var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.login',true,true);e.success=false;e.token='';e.cancelled={0};document.dispatchEvent(e);", "true");
                    }
                    InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);

                }
                catch (Exception ex)
                {
                    string js = String.Format("var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.login',true,true);e.success=false;e.token='';e.cancelled={0};document.dispatchEvent(e);", "true");
                    InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
                }


            });
        }

        private async Task ShowAppRequestDialog(string parameters)
        {
            string js2 = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.error='Not implemented in wp8.';e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);";
            InvokeCustomScript(new ScriptCallback("eval", new string[] { js2 }), true);
            resetFBStatus();
            return;

            try
            {
                string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);

                //{"to":"appmobi.ryan.5","message":"My Awesome Message","title":"A title for this dialog would go here"}

                var oParams = JsonHelper.Deserialize<AppRequestItem>(args[0]);

                string request = String.Format("/me/apprequests");
                dynamic fbPostTaskResult = await facebookClient.PostTaskAsync(request, oParams);
                var result = (IDictionary<string, object>)fbPostTaskResult;

                string js = "";

                string requestId = result["request"].ToString();

                if (requestId != null)
                {
                    string extra = "";

                    request = requestId;
                    extra += "e.request=" + request + ";";

                    if (result.Keys.Contains("to"))
                    {
                        JsonArray to = (JsonArray)result["to"];

                        extra += "e.to=[";
                        for (int i = 0; i < to.Count; i++)
                        {
                            try
                            {
                                extra += "\"" + to[i] + "\",";
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        extra += "];";
                    }

                    js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=true;e.error='';e.raw='" + result.ToString() + "';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}" + extra + "document.dispatchEvent(e);";
                    //webView.loadUrl(js);
                    //resetFBStatus();
                }
                else
                {
                    js = String.Format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);");
                }

                InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
            }
            catch (Exception ex)
            {
                string js = String.Format("var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=false;e.error='{0}';e.raw='';e.data={};document.dispatchEvent(e);", ex.ToString());
                InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
            }



        }

        private async Task RequestWithGraphAPI(string parameters)
        {
            string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);

            string path = args[0];
            string method = args[1];
            string paramss = args[2];

            try
            {
                // additional parameters can be passed and 
                // must be assignable from IDictionary<string, object>
                var fbParams = new Dictionary<string, object>();
                //fbParams["fields"] = "first_name,last_name";

                dynamic friendsTaskResult = await facebookClient.GetTaskAsync(path, fbParams);
                var result = (IDictionary<string, object>)friendsTaskResult;

                //string responsestr = result["data"].ToString().Replace("'", "\\\\'");
                string responsestr = result.ToString();   //.Replace("'", "\\'");
                //responsestr = responsestr.Replace("\"", "\\\\\"");

                string js = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=true;e.raw=JSON.stringify(" + responsestr + ");e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){alert(ex);}e.error='';document.dispatchEvent(e);";
                InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
            }
            catch (FacebookApiException ex)
            {
                string js = String.Format("var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='{0}';e.raw='';e.data={};document.dispatchEvent(e);", ex.ToString());
                InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
            }
        }

        private async Task ShowNewsFeedDialog(string parameters)
        {
            string js2 = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.error='Not implemented in wp8.';e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);";
            InvokeCustomScript(new ScriptCallback("eval", new string[] { js2 }), true);
            resetFBStatus();
            return;

            string[] args = WPCordovaClassLib.Cordova.JSON.JsonHelper.Deserialize<string[]>(parameters);

            var oParams = JsonHelper.Deserialize<NewsFeedItem>(args[0]);

            //var oParams = JsonHelper.Deserialize<object>(args[0].Replace("{", "").Replace("}", ""));
            //string[] oParams = args[0].Replace("{", "").Replace("}", "").Split(',');

            //{"picture":"http://fbrell.com/f8.jpg","name":"Facebook Dialog","caption":"This is my caption","description":"Using Dialogs to interact with users.","link":"http://xdk.intel.com","message":"My test message!"}

            //var postParams = new
            //{
            //    name = oParams.name,
            //    caption = oParams.caption,
            //    description = oParams.description,
            //    link = oParams.link,
            //    picture = oParams.picture,
            //    message = oParams.message
            //};

            try
            {
                dynamic fbPostTaskResult = await facebookClient.PostTaskAsync("/me/feed", oParams);
                var result = (IDictionary<string, object>)fbPostTaskResult;

                string postId = result["id"].ToString();
                if (postId != null && postId != "") {

                    string extra ="";
                                                
                    try {
                        string request = result.ToString();
                        extra += "e.request="+request+";";
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        //e1.printStackTrace();
                    }

                    if (result.Keys.Contains("to"))
                    {
                        JsonArray to = (JsonArray) result["to"];

                        extra += "e.to=[";
                        for (int i = 0; i < to.Count; i++)
                        {
                            try
                            {
                                extra += "\"" + to[i] + "\",";
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        extra += "];";
                    }

                    string js = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=true;e.error='';e.raw='" + result.ToString() + "';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}" + extra + "document.dispatchEvent(e);";
                    InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
                    resetFBStatus();
                } else {
                    // User clicked the Cancel button
                    string js = "var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);";
                    InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
                    resetFBStatus();
                }
            }
            catch (Exception ex)
            {
                string js = String.Format("var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=false;e.error='{0}';e.raw='';e.data={};document.dispatchEvent(e);", ex.ToString());
                InvokeCustomScript(new ScriptCallback("eval", new string[] { js }), true);
            }
        }

        private void resetFBStatus()
        {
            busy = false;
            currentCommand = 0;
            currentCommandArguments = new String[] { };
        }
        #endregion
    }

    public class AppRequestItem
    {
        public string to { get; set; }
        public string message { get; set; }
        public string title { get; set; }
    }

    public class NewsFeedItem
    {
        public string picture { get; set; }
        public string name { get; set; }
        public string caption { get; set; }
        public string description { get; set; }
        public string link { get; set; }
        public string message { get; set; }
    }
}
