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

package com.intel.xdk.facebook;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.model.GraphObject;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;


@SuppressWarnings("deprecation")
public class IntelXDKFacebook extends CordovaPlugin implements CordovaInterface{
	
	public boolean busy = false;
	public int currentCommand = 0;
	public String[] currentCommandArguments;
	//public static final String APP_ID = activity.getString(R.string.FBAppId);
	private static final String defaultLoginPermissions = "publish_stream,publish_actions,offline_access";
	
	private Session.StatusCallback statusCallback = new SessionStatusCallback();
	
	public static final int LOGIN = 1;
	public static final int REQ_GRAPH_API = 2;
	public static final int REQ_REST_API = 3;
	public static final int APP_REQUEST_DIALOG = 4;
	public static final int NEWS_FEED_DIALOG = 5;

	public static final int FACEBOOK_AUTHORIZE = 9;
	
	private String useFrictionless = "0";
	
	private Session session;
	
	private Activity activity;

	//Facebook facebook;
	//AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);

	private CallbackContext callbackContext;
	
	//this is needed to work around the resource binding problem
	private static Context sharedContext;
	public static int getId(String type, String name) {
		return sharedContext.getResources().getIdentifier(name, type, sharedContext.getPackageName());
	}
	//http://stackoverflow.com/questions/13816596/accessing-declare-styleable-resources-programatically
	public static final int[] getStyleableIntArray( String name )
	{
	    try
	    {
	        Field[] fields2 = Class.forName( sharedContext.getPackageName() + ".R$styleable" ).getFields();
	        for ( Field f : fields2 )
	        {
	            if ( f.getName().equals( name ) )
	            {
	                int[] ret = (int[])f.get( null );
	                return ret;
	            }
	        }
	    }
	    catch ( Throwable t ){}
	    return null;
	}
	
	public IntelXDKFacebook(){
		//String ryan = "val";
		//facebook = new Facebook(activity.getString(R.string.FBAppId));
	}

	@Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        
        //get convenience reference to activity
        activity = cordova.getActivity();
        sharedContext = activity;
        
		cordova.setActivityResultCallback(this);

        /*this.session = Session.getActiveSession();
        if (this.session == null) {
        	this.session = new Session(activity);
        }*/
        
/*		StatusCallback callback = new StatusCallback() {
            public void call(Session session, SessionState state, Exception exception) {
                if (exception != null) {
                   
                }
            }
		};*/

        //Session.setActiveSession(this.session);
        this.session = createSession();
        /*if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            session.openForRead(new Session.OpenRequest(activity).setCallback(callback));
        }*/

	}

	@Override
	public void setActivityResultCallback(CordovaPlugin plugin) {
	}
	
/**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True when the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("enableFrictionlessRequests")) {
        	this.enableFrictionlessRequests();
        }
        else if (action.equals("login")) {
        	this.login(args.getString(0));
        }
        else if (action.equals("logout")) {
        	this.logout();
        }
        else if (action.equals("requestWithGraphAPI")) {
            //this.callbackContext = callbackContext;
        	this.requestWithGraphAPI(new String[] {args.getString(0), args.getString(1), args.getString(2)});
        }
        else if (action.equals("requestWithRestAPI")) {
        	//this.requestWithRestAPI(args.toString().replace("[", "").replace("]", "").split(","));
        	if(Debug.isDebuggerConnected()) Log.d("[IntelXCKFacebook]", "requestWithRestAPI: Has been deprecated.");
			String js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='Facebook REST APIs have been deprecated.';e.raw='';e.data={};document.dispatchEvent(e);";
        	//webView.loadUrl(js);
			injectJS(js);
        }
        else if (action.equals("showAppRequestDialog")) {
        	this.showAppRequestDialog(args.getString(0));        	
        }
        else if (action.equals("showNewsFeedDialog")) {
        	this.showNewsFeedDialog(args.getString(0));        	
        }
        else {
            return false;
        }

        // All actions are async.
        //callbackContext.success();
        return true;
    }

		public void authorizeCallback(int requestCode, int resultCode, Intent data)
	{
			String alex = "mason";
		//facebook.authorizeCallback(requestCode, resultCode, data);
	}
	
	//placeholder - this is available on iOS but not on Android
	@JavascriptInterface
	public void enableFrictionlessRequests() {
		this.useFrictionless = "1";
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	      super.onActivityResult(requestCode, resultCode, data);
	      
	      //switch (requestCode) {
	      //case RESULT_CODE_IMPORT_FACEBOOK:
	            //do stuff
	      //      break;
	      //default:
	            Session.getActiveSession().onActivityResult(activity, requestCode, resultCode, data);
	      //      break;
	      //}
	  }
	
	//LOGOUT
	//------------------------------------------
	@JavascriptInterface
	public void logout()
	{
		if(busy)
		{
			sendBusyEvent();
			return;
		}

		Session session = Session.getActiveSession();
        if (session.isOpened()) {
    		busy=true;

    		session.closeAndClearTokenInformation();
			String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.logout',true,true);e.success=%s;document.dispatchEvent(e);", "true");
        	//webView.loadUrl(js);
        	injectJS(js);

 			resetFBStatus();
        }
	}
	
	
	//LOGIN
	//-------------------------------------------
	@JavascriptInterface
	public void login(final String permissions)
	{
		if(busy)
		{
			sendBusyEvent();
			return;
		}
		busy=true;
		
		if(currentCommand == 0)
			currentCommand = LOGIN;
		
		
				String[] perms  = permissions.split("\\s*,\\s*");
				//facebook.authorize(activity, new String[] {}, AppMobiActivity.FACEBOOK_AUTHORIZE, new Facebook.DialogListener() 
				//facebook.authorize(activity, perms, FACEBOOK_AUTHORIZE, FBLoginListener);

				// start Facebook Login
				//Session.openActiveSession(activity, true, statusCallback);
				
//				Session session = Session.getActiveSession();
//				session.openForRead(new Session.OpenRequest(activity).setCallback(statusCallback));
			    
				/*Session.openActiveSession(activity, true, new Session.StatusCallback() {

			      // callback when session changes state
			      @Override
			      public void call(Session session, SessionState state, Exception exception) {
			        if (session.isOpened()) {

			          // make request to the /me API
			          Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

			            // callback after Graph API response with user object
			            @Override
			            public void onCompleted(GraphUser user, Response response) {
			              if (user != null) {
			                
			              }
			            }
			          });
			        }
			      }
			    });*/
				
				Session.StatusCallback callback = new StatusCallback() {
	                public void call(Session session, SessionState state, Exception exception) {
	                    if (exception != null) {
	                       
	                    }
	                }
				};
	                
				this.session = Session.getActiveSession();
				if (this.session == null) {
					this.session = new Session(activity);
		        }
		        if (!this.session.isOpened() && !this.session.isClosed()) {
		            session.openForRead(new Session.OpenRequest(activity).setCallback(statusCallback));
		            //this.session.openForRead(new Session.OpenRequest(activity).setCallback(callback));
		        } else {
		            Session.openActiveSession(activity, true, statusCallback);
		        }
	}
	
    /*private void updateView() {
        Session session = Session.getActiveSession();
        if (session.isOpened()) {
        } else {
        }
    }*/

	 private class SessionStatusCallback implements Session.StatusCallback {
	        @Override
	        public void call(Session session, SessionState state, Exception exception) {
	        	switch (session.getState()) {
				case CLOSED:
					break;

				case CLOSED_LOGIN_FAILED:
					processAutoLogin(false, true);
					break;

	        	case CREATED:
	        		break;
	        		
	        	case CREATED_TOKEN_LOADED:
	        		break;
	        		
	        	case OPENED:
					processAutoLogin(true, false);
	        		break;
	        		
	        	case OPENED_TOKEN_UPDATED:
	        		break;

	        	case OPENING:
					break;
	        		
				default:
					break;
				}
	        }
	    }
	 
	public void processAutoLogin(boolean success, boolean cancelled)
	{
		busy=false;
		String js = "";
		
		//if(success)
		//	webView.loadUrl("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.login',true,true);e.success=true;e.cancelled=false;e.token='"+facebook.getAccessToken()+"';document.dispatchEvent(e);");
		
		switch( currentCommand )
        {	
			case LOGIN:
				if(!success)
					js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.login',true,true);e.success=false;e.token='';e.cancelled=%s;document.dispatchEvent(e);", (cancelled?"true":"false"));
				else
					js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.login',true,true);e.success=true;e.token='';e.cancelled=%s;document.dispatchEvent(e);", "false");
				break;			
        	case REQ_GRAPH_API:
        		if( success)
        			requestWithGraphAPI(currentCommandArguments);
        		else
        			js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='login failed';e.raw='';e.data={};document.dispatchEvent(e);";
        		break;
        	case REQ_REST_API:
        		if( success)
        			requestWithRestAPI(currentCommandArguments);
        		else
        			js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='login failed';e.raw='';e.data={};document.dispatchEvent(e);";
        		break;
        	case APP_REQUEST_DIALOG:
        		if( success)
        			showAppRequestDialog(currentCommandArguments[0]);
        		else
        			js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.fail',true,true);e.success=false;e.error='login failed';e.raw='';e.data={};document.dispatchEvent(e);";
        		break;
        	case NEWS_FEED_DIALOG:
        		if( success)
        			showNewsFeedDialog(currentCommandArguments[0]);
        		else
        			js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.fail',true,true);e.success=false;e.error='login failed';e.raw='';e.data={};document.dispatchEvent(e);";
        		break;
        }
		
		//SEND THE JS TO THE DEVICE
		if(js.length() > 0)
			injectJS(js);
			//webView.loadUrl(js);
		
		busy = false;resetFBStatus();
	}
	
	public void sendBusyEvent()
	{
		String js = "javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.busy',true,true);e.success=false;e.message='busy';document.dispatchEvent(e);";
		//webView.loadUrl(js);
		injectJS(js);
	}
	
	
	
	
	//JSON To BUNDLE
	//---------------------------------------------------------
	private Bundle JSONtoBundle(JSONObject jsonObj)
	{
		Bundle params  = new Bundle();
		Iterator<?> iter = jsonObj.keys();
	    while(iter.hasNext())
	    {
	        String key = (String)iter.next();
	        String value;
			try
			{
				value = jsonObj.getString(key);
				params.putString(key, value);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
	    }    
		return params;
	}
	
	//BUNDLE TO JSON
	//---------------------------------------------------------
	private JSONObject BundleToJSON(Bundle bundleObj)
	{
		JSONObject jsonObj = new JSONObject();
			
			Object[] keys =  bundleObj.keySet().toArray();
			
			
			for(int i = 0; i < keys.length; i++)
			{
				String key = (String) keys[i];
				try {
					jsonObj.put(key, bundleObj.get(key));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			
		
		return jsonObj;
	}
	
	
	// REST API REQUEST
	//---------------------------------------------------------
	@JavascriptInterface
	public void requestWithRestAPI(String[] arguments)
	{
		if(busy)
		{
			sendBusyEvent();
			return;
		}
 
		if(!session.isOpened())
		{
			currentCommand = REQ_REST_API;
			currentCommandArguments = arguments;
			login(defaultLoginPermissions);
			return;
		}
		else
		{
			busy=true;
		}
		
		
		String command = arguments[0].replace("\"", "");
		String method = arguments[1].replace("\"", "");
		String parameters = arguments[2];
		
		JSONObject json = new JSONObject();
		try {
			json = new JSONObject(parameters);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		try {
//			json.put("method", "auth.expireSession");
//		} catch (JSONException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		
		Bundle params = new Bundle();
//		params.putString("caption", "caption");
//		params.putString("message", "message");
//		params.putString("link", "link_url");
//		params.putString("picture", "picture_url");
		//params.putString("fields", "id,name,picture");
		
		//MAKE THE REQUEST
		//mAsyncRunner.request(null, JSONtoBundle(json), method, new FBApiRequestListener(),true);
		Request restRequest = Request.newRestRequest(session, command, JSONtoBundle(json), (method.toUpperCase().equals("GET")) ? HttpMethod.GET : HttpMethod.POST);
		//Request restRequest = Request.newRestRequest(session, "me/" + command, params, HttpMethod.POST);
		restRequest.setCallback( new Request.Callback() 
		{

		    @Override
		    public void onCompleted(Response response) 
		    {
                FacebookRequestError error = response.getError();
                if (error != null) {
                	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", error.toString());
                	//webView.loadUrl(js);
                	injectJS(js);
        			resetFBStatus();
        			return;
//        			if (error instanceof FacebookOperationCanceledException) {
//        			}
//        			else {
//        				
//        			}
                } else if (session == Session.getActiveSession()) {
                	String responsestr = response.toString().replaceAll("'", "\\\\'");
                	responsestr = responsestr.replaceAll("\"", "\\\\\"");
                	
                	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=true;e.raw='%s';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}e.error='';document.dispatchEvent(e);", responsestr);
                	//webView.loadUrl(js);
                	injectJS(js);
        			resetFBStatus();
        			return;
                }
		    }
		});
		//Request.executeBatchAsync(restRequest);
		restRequest.executeAndWait();
	}
	
	// GRAPH API REQUEST
	//---------------------------------------------------------
	@JavascriptInterface
	public void requestWithGraphAPI(String[] arguments)// path, String method, String parameters)
	{
		if(busy)
		{
			sendBusyEvent();
			return;
		}
		
        if (!session.isOpened())
		{
			currentCommand = REQ_GRAPH_API;
			currentCommandArguments = arguments;
			login(defaultLoginPermissions);
			return;
		}
		else
		{
			busy=true;
		}
		
		final String path = arguments[0];
		String method = arguments[1];
		String parameters = arguments[2];
		
		JSONObject json = new JSONObject();
		try {
			json = new JSONObject(parameters);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		final JSONObject finalJson = json;
		
		cordova.getThreadPool().execute(new Runnable() {
		    public void run() {		
		
		        Request graphRequest = Request.newGraphPathRequest(session, path, new Request.Callback() {
					@Override
					public void onCompleted(Response response) {
		                FacebookRequestError error = response.getError();
		                if (error != null) {
		                	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", error.toString());
		                	//webView.loadUrl(js);
		                	injectJS(js);
		        			resetFBStatus();
		        			return;
		                } else if (session == Session.getActiveSession()) {
		                	GraphObject graphObject = response.getGraphObject();
		                	JSONArray array;
		                	
		                	if (graphObject != null) {
		                        JSONObject jsonObject = graphObject.getInnerJSONObject();

			                	String responsestr = jsonObject.toString().replaceAll("'", "\\\\'");
			                	responsestr = responsestr.replaceAll("\"", "\\\\\"");
			                	
			                	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=true;e.raw='%s';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}e.error='';document.dispatchEvent(e);", responsestr);
			                	//webView.loadUrl(js);
			                	injectJS(js);
			        			resetFBStatus();
			        			return;
	                		} else {
			                	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", "There was a problem with the FB graph call.");
			                	//webView.loadUrl(js);
			                	injectJS(js);
			        			resetFBStatus();
			        			return;
	                		}
		                	
		                }
					}
				});
		        Bundle params = JSONtoBundle(finalJson);
		        //params.putString("fields", "name,first_name,last_name");
		        graphRequest.setParameters(params);
		        graphRequest.executeAndWait();
        
		    }});
	}

	// APP REQUEST DIALOG
	//---------------------------------------------------------
	@JavascriptInterface
	public void showAppRequestDialog(final String parameters)
	{	
		if(!session.isOpened())
		{
			currentCommand = APP_REQUEST_DIALOG;
			currentCommandArguments = new String[]{parameters};
			login(defaultLoginPermissions);
			return;
		}
		else
		{
			//busy=true;
		}
		
		activity.runOnUiThread(new Runnable() {
			//@Override
			public void run()
			{
				
				JSONObject json = new JSONObject();
				try {
					json = new JSONObject(parameters);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			    Bundle params = JSONtoBundle(json);
			    
			    params.putString("frictionless", useFrictionless);
			    
			    WebDialog requestsDialog = (
			        new WebDialog.RequestsDialogBuilder(activity,
			            session,
			            params))
			            .setOnCompleteListener(new OnCompleteListener() {

			                @Override
			                public void onComplete(Bundle values,
			                    FacebookException error) {
			                    if (error != null) {
			                        if (error instanceof FacebookOperationCanceledException) {
			                        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);");
			                        	webView.loadUrl(js);
			                 			resetFBStatus();
			                        } else {
			                        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", error.toString());
			                        	webView.loadUrl(js);
			                 			resetFBStatus();
			                        }
			                    } else {
			                        final String requestId = values.getString("request");
			                        if (requestId != null) {
			                			JSONObject jsonData = BundleToJSON(values);
			                			String extra ="";
			                						
			                			try {
			                				String request = jsonData.getString("request");
			                				extra += "e.request="+request+";";
			                			} catch (JSONException e1) {
			                				// TODO Auto-generated catch block
			                				e1.printStackTrace();
			                			}
			                				
			                			int index = 0;
			                			if(jsonData.has("to["+index+"]"))
			                			{
			                				extra += "e.to=[";
			                				while(jsonData.has("to["+index+"]"))
			                				{
			                					try {extra += jsonData.getString("to["+index+"]")+",";} catch (JSONException e) {}
			                					index++;
			                				}
			                				extra += "];";
			                			}
			                			
			                			String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=true;e.error='';e.raw='%s';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}%sdocument.dispatchEvent(e);", jsonData.toString(), extra);
			                			webView.loadUrl(js);
			                			resetFBStatus();
			                        } else {
			                        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);");
			                        	webView.loadUrl(js);
			                 			resetFBStatus();
			                        }
			                    }   
			                }

			            })
			            .build();
			    requestsDialog.show();
			}    
		});
			
	}
	
	
	//NEWS FEED DIALOG
	//---------------------------------------------------------
	@JavascriptInterface
	public void showNewsFeedDialog(final String parameters)
	{	
		if(!session.isOpened())
		{
			currentCommand = NEWS_FEED_DIALOG;
			currentCommandArguments = new String[]{parameters};
			login(defaultLoginPermissions);
			return;
		}
		else
		{
			busy=true;
		}
		
		activity.runOnUiThread(new Runnable() {
			//@Override
			public void run()
			{
				
				JSONObject json = new JSONObject();
				try {
					json = new JSONObject(parameters);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			  //facebook.dialog(activity, "feed", JSONtoBundle(json), FBDialogListener );
//				Bundle params = new Bundle();
//			    params.putString("name", "Facebook SDK for Android");
//			    params.putString("caption", "Build great social apps and get more installs.");
//			    params.putString("description", "The Facebook SDK for Android makes it easier and faster to develop Facebook integrated Android apps.");
//			    params.putString("link", "https://developers.facebook.com/android");
//			    params.putString("picture", "https://raw.github.com/fbsamples/ios-3.x-howtos/master/Images/iossdk_logo.png");
				Bundle params = JSONtoBundle(json);
				
			    params.putString("frictionless", useFrictionless);

			    WebDialog feedDialog = (
			        new WebDialog.FeedDialogBuilder(activity,
			            session,
			            params))
			        .setOnCompleteListener(new OnCompleteListener() {

			            @Override
			            public void onComplete(Bundle values,
			                FacebookException error) {
			                if (error == null) {
			                    // When the story is posted, echo the success
			                    // and the post Id.
			                    final String postId = values.getString("post_id");
			                    if (postId != null) {
//			                        Toast.makeText(activity,
//			                            "Posted story, id: "+postId,
//			                            Toast.LENGTH_SHORT).show();
		                			JSONObject jsonData = BundleToJSON(values);
		                			String extra ="";
		                						
		                			try {
		                				String request = jsonData.getString("request");
		                				extra += "e.request="+request+";";
		                			} catch (JSONException e1) {
		                				// TODO Auto-generated catch block
		                				e1.printStackTrace();
		                			}
		                				
		                			int index = 0;
		                			if(jsonData.has("to["+index+"]"))
		                			{
		                				extra += "e.to=[";
		                				while(jsonData.has("to["+index+"]"))
		                				{
		                					try {extra += jsonData.getString("to["+index+"]")+",";} catch (JSONException e) {}
		                					index++;
		                				}
		                				extra += "];";
		                			}
		                			
		                			String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=true;e.error='';e.raw='%s';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}%sdocument.dispatchEvent(e);", jsonData.toString(), extra);
		                			webView.loadUrl(js);
		                			resetFBStatus();
			                    } else {
			                        // User clicked the Cancel button
//			                        Toast.makeText(activity.getApplicationContext(), 
//			                            "Publish cancelled", 
//			                            Toast.LENGTH_SHORT).show();
		                        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);");
		                        	webView.loadUrl(js);
		                 			resetFBStatus();
			                    }
			                } else if (error instanceof FacebookOperationCanceledException) {
			                    // User clicked the "x" button
//			                    Toast.makeText(activity.getApplicationContext(), 
//			                        "Publish cancelled", 
//			                        Toast.LENGTH_SHORT).show();
	                        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);");
	                        	webView.loadUrl(js);
	                 			resetFBStatus();
			                } else {
			                    // Generic, ex: network error
//			                    Toast.makeText(activity.getApplicationContext(), 
//			                        "Error posting story", 
//			                        Toast.LENGTH_SHORT).show();
	                        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", error.toString());
	                        	webView.loadUrl(js);
	                 			resetFBStatus();
			                }
			            }

			        })
			        .build();
			    feedDialog.show();			
			}
		});
			
	}
		
	private void resetFBStatus()
	{
		busy = false;
		currentCommand = 0;
		currentCommandArguments = new String[]{};
	}

	
	
	//LOGIN DIALOG LISTENER
	//----------------------------------------------------------------------------
	private Facebook.DialogListener FBLoginListener = new Facebook.DialogListener()
	{
		@Override
		public void onFacebookError(FacebookError error)
		{
       	   	processAutoLogin(false,false);
       	   	Log.d("[intel.xdk]",  "Facebook Error: " + error.getMessage());
			return;
        }

        @Override
        public void onError(DialogError e)
        {
        	processAutoLogin(false,false);
       	   	Log.d("[intel.xdk]",  "Facebook Error: " + e.getMessage());
 			return;
        }

        @Override
        public void onCancel()
        {
        	processAutoLogin(false,true);
  			return;
        }

		@Override
		public void onComplete(Bundle values)
		{
           processAutoLogin(true,false);
           return;
		}
	};

    private Session createSession() {
        Session activeSession = Session.getActiveSession();
        if (activeSession == null || activeSession.getState().isClosed()) {
            //activeSession = new Session.Builder(activity).setApplicationId(APP_ID).build();
            activeSession = new Session.Builder(activity).build();
            Session.setActiveSession(activeSession);
        }
        return activeSession;
    }


	@Override
	public Activity getActivity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ExecutorService getThreadPool() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void startActivityForResult(CordovaPlugin arg0, Intent arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	
	//DIALOG RESULT LISTENER/HANDLER
	//----------------------------------------------------------------------------
	private Facebook.DialogListener FBDialogListener = new Facebook.DialogListener()
	 {
        @Override
        public void onFacebookError(FacebookError error)
        {
        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", error.toString());
        	webView.loadUrl(js);
 			resetFBStatus();
 			return;
        }

        @Override
        public void onError(DialogError e)
        {
        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", e.toString());
        	webView.loadUrl(js);
 			resetFBStatus();
 			return;
        }

        @Override
        public void onCancel()
        {
        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.canceled='true';e.success=false;e.raw='';e.data={};document.dispatchEvent(e);");
        	webView.loadUrl(js);
 			resetFBStatus();
 			return;
        }

		@Override
		public void onComplete(Bundle values)
		{
			JSONObject jsonData = BundleToJSON(values);
			String extra ="";
						
			try {
				String request = jsonData.getString("request");
				extra += "e.request="+request+";";
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
				
			int index = 0;
			if(jsonData.has("to["+index+"]"))
			{
				extra += "e.to=[";
				while(jsonData.has("to["+index+"]"))
				{
					try {extra += jsonData.getString("to["+index+"]")+",";} catch (JSONException e) {}
					index++;
				}
				extra += "];";
			}
			
			String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.dialog.complete',true,true);e.success=true;e.error='';e.raw='%s';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}%sdocument.dispatchEvent(e);", jsonData.toString(), extra);
			webView.loadUrl(js);
			resetFBStatus();
			return;
       }
    };
    
    private void injectJS(final String js) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                webView.loadUrl(js);
            }
        });
    }
    
	//API REQUEST LISTENER
	//----------------------------------------------------------------------------
/*	public class FBApiRequestListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
        	String responsestr = response.replaceAll("'", "\\\\'");
        	responsestr = responsestr.replaceAll("\"", "\\\\\"");
        	
        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=true;e.raw='%s';e.data={};try{e.data=JSON.parse(e.raw);}catch(ex){}e.error='';document.dispatchEvent(e);", responsestr);
        	webView.loadUrl(js);
			resetFBStatus();
			return;
        }

        public void onFacebookError(FacebookError error) {
        	String js = String.format("javascript: var e = document.createEvent('Events');e.initEvent('intel.xdk.facebook.request.response',true,true);e.success=false;e.error='%s';e.raw='';e.data={};document.dispatchEvent(e);", error.toString());
        	webView.loadUrl(js);
			resetFBStatus();
			return;
        }

    }*/

	
	
	//GENERIC REQUEST LISTENER CLASS
/*	public abstract class BaseRequestListener implements RequestListener {

	    @Override
	    public void onFacebookError(FacebookError e, final Object state) {
	        Log.e("Facebook", e.getMessage());
	        e.printStackTrace();
	    }

	    @Override
	    public void onFileNotFoundException(FileNotFoundException e, final Object state) {
	        Log.e("Facebook", e.getMessage());
	        e.printStackTrace();
	    }

	    @Override
	    public void onIOException(IOException e, final Object state) {
	        Log.e("Facebook", e.getMessage());
	        e.printStackTrace();
	    }

	    @Override
	    public void onMalformedURLException(MalformedURLException e, final Object state) {
	        Log.e("Facebook", e.getMessage());
	        e.printStackTrace();
	    }
	}*/
}