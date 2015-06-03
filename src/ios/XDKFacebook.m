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

#import "Cordova/CDV.h"
#import <UIKit/UIKit.h>
#import "FBConnect.h"
#import "FBDelegate.h"

// "if(1)" turns OFF XDKog logging.
// "if(0)" turns ON XDKog logging.
#define XDKLog if(0); else NSLog

@class XDKFacebook;
typedef void (^DeferredCommand)(XDKFacebook* mySelf);

@interface XDKFacebook : CDVPlugin < FBSessionDelegate
                                   , FBRequestDelegate
                                   , FBDialogDelegate
                                   >
{
    //! YES if some Facebook activity has been started and has not completed. (Means "don't
    //! start a new activity".)
    BOOL                _busy;
    
	BOOL                _willHandleUserTouchingLink;
    
    //! Code to execute after a successful login.
    DeferredCommand     _toDoAfterLogin;
    
    //! Event name for a failure event to fire after a failed login.
    NSString*           _eventAfterLogin;
}

@end


@implementation XDKFacebook

- (void)pluginInitialize
{
	[FBDelegate addSessionListener:self];
    _busy = NO;
	_willHandleUserTouchingLink = NO;
    _toDoAfterLogin = nil;
    _eventAfterLogin = nil;
}


#pragma mark Commands

- (void)login:(CDVInvokedUrlCommand*)command
{
    if ([self isBusy]) return;

	NSString* permissionsString = [command argumentAtIndex:0 withDefault:@""];
	NSArray* permissions = [permissionsString componentsSeparatedByString:@","];
	[[FBDelegate sharedInstance] login:permissions];
}


- (void)logout:(CDVInvokedUrlCommand*)command
{
    if ([self isBusy]) return;
	[[FBDelegate sharedInstance] logout]; //should use version with parameter to handle 3.1.3?
}


- (void)requestWithRestAPI:(CDVInvokedUrlCommand*)command
{
    if ([self isBusy]) return;

	NSString* methodName = [command argumentAtIndex:0];
	NSString* httpMethod = [[command argumentAtIndex:1] uppercaseString];
    NSString* paramStr = [command argumentAtIndex:2 withDefault:@""];
	NSMutableDictionary* params = [NSMutableDictionary dictionaryWithDictionary:
                                   paramStr.length == 0 ? @{} :
                                   [[SBJSON new] objectWithString:paramStr]];
    
	[self doWhenLoggedInEventName:@"facebook.request.response"
                           action:^(XDKFacebook* mySelf) {
        [[FBDelegate sharedInstance].fb requestWithMethodName:methodName
                                                    andParams:params
                                                andHttpMethod:httpMethod
                                                  andDelegate:mySelf];
    }];
}

- (void)requestWithGraphAPI:(CDVInvokedUrlCommand*)command
{
    if ([self isBusy]) return;
    
	NSString* path = [command argumentAtIndex:0];
	NSString* method = [[command argumentAtIndex:1] uppercaseString];
    NSString* paramStr = [command argumentAtIndex:2 withDefault:@""];
	NSMutableDictionary* params = [NSMutableDictionary dictionaryWithDictionary:
                                   paramStr.length == 0 ? @{} :
                                   [[SBJSON new] objectWithString:paramStr]];
	
    [self doWhenLoggedInEventName:@"facebook.request.response"
                           action:^(XDKFacebook* mySelf) {
        [[FBDelegate sharedInstance].fb requestWithGraphPath:path
                                                   andParams:params
                                               andHttpMethod:method
                                                 andDelegate:mySelf];
                           }];
}


//title, message, filters, to, notification_text, data
- (void)showAppRequestDialog:(CDVInvokedUrlCommand*)command
{
    if ([self isBusy]) return;
    
    NSString* paramStr = [command argumentAtIndex:2 withDefault:@""];
	NSMutableDictionary* params = [NSMutableDictionary dictionaryWithDictionary:
                                   paramStr.length == 0 ? @{} :
                                   [[SBJSON new] objectWithString:paramStr]];
    
    NSArray* keys = [params allKeys];
    for (NSString* key in keys) {
        if (! [params[key] isKindOfClass:[NSString class]]) {
            [params removeObjectForKey:key];
        }
    }
    
    [self doWhenLoggedInEventName:@"facebook.dialog.fail"
                           action:^(XDKFacebook* mySelf) {
            [[FBDelegate sharedInstance].fb dialog:@"apprequests"
                                         andParams:params
                                       andDelegate:mySelf];
        }];
}


//from, to, link, picture, source, name, caption, description, properties(JSON), actions(JSON), ref
//message
- (void)showNewsFeedDialog:(CDVInvokedUrlCommand*)command
{
    if ([self isBusy]) return;
    
    NSString* paramStr = [command argumentAtIndex:2 withDefault:@""];
	NSMutableDictionary* params = [NSMutableDictionary dictionaryWithDictionary:
                                   paramStr.length == 0 ? @{} :
                                   [[SBJSON new] objectWithString:paramStr]];
    
    NSArray* keys = [params allKeys];
    for (NSString* key in keys) {
        if (! [params[key] isKindOfClass:[NSString class]]) {
            [params removeObjectForKey:key];
        }
    }
    
    [self doWhenLoggedInEventName:@"facebook.dialog.fail"
                           action:^(XDKFacebook* mySelf) {
            [[FBDelegate sharedInstance].fb dialog:@"feed"
                                         andParams:params
                                       andDelegate:mySelf];
        }];
}

- (void)enableFrictionlessRequests:(CDVInvokedUrlCommand*)command
{
    [[FBDelegate sharedInstance].fb enableFrictionlessRequests];
}

- (void)setWillHandleUserTouchingLink:(CDVInvokedUrlCommand*)command
{
	_willHandleUserTouchingLink = [[command argumentAtIndex:0] boolValue];
}


#pragma mark - FBSessionDelegate methods

/**
 * Called when the user successfully logged in.
 */
- (void)fbDidLogin {
    [self fireEvent:@"facebook.login"
            success:YES
         components:@{ @"token": quotedString([FBDelegate sharedInstance].fb.accessToken),
                       @"cancelled": @"false" }];
    if (_toDoAfterLogin) {
        _toDoAfterLogin(self);
        _toDoAfterLogin = nil;
        _eventAfterLogin = nil;
    }
    else {
        _busy = NO;
    }
}


/**
 * Called when the user dismissed the dialog without logging in.
 */
- (void)fbDidNotLogin:(BOOL)cancelled {
    [self fireEvent:@"facebook.login"
            success:NO
         components:@{ @"token": @"''",
                       @"cancelled": (cancelled? @"true" : @"false") }];
    [self fireEvent:_eventAfterLogin
            success:NO
         components:@{ @"error": @"'login failed'",
                       @"data": @"{}",
                       @"raw": @"''" }];
    _toDoAfterLogin = nil;
    _eventAfterLogin = nil;
    _busy = NO;
}


/**
 * Called after the access token was extended. If your application has any
 * references to the previous access token (for example, if your application
 * stores the previous access token in persistent storage), your application
 * should overwrite the old access token with the new one in this method.
 * See extendAccessToken for more details.
 */
- (void)fbDidExtendToken:(NSString*)accessToken expiresAt:(NSDate*)expiresAt {
}

/**
 * Called when the user logged out.
 */
- (void)fbDidLogout {
    [self fireEvent:@"facebook.logout" success:YES components:nil];
    _busy = NO;
}

/**
 * Called when the current session has expired. This might happen when:
 *  - the access token expired
 *  - the app has been disabled
 *  - the user revoked the app's permissions
 *  - the user changed his or her password
 */
- (void)fbSessionInvalidated {
    [self fireEvent:@"facebook.session.invalidate" success:YES components:nil];
}


#pragma mark - FBRequestDelegate methods

/**
 * Called when an error prevents the request from completing successfully.
 */
- (void)request:(FBRequest *)request didFailWithError:(NSError *)error
{
	XDKLog(@"request:didFailWithError for response: %@", [error description]);
    [self fireEvent:@"facebook.request.response"
            success:NO
         components:@{ @"data": @"{}",
                       @"raw": @"''",
                       @"error": quotedString([error localizedDescription]) }];
    _busy = NO;
}


/**
 * Called when a request returns and its response has been parsed into
 * an object.
 *
 * The resulting object may be a dictionary, an array, a string, or a number,
 * depending on thee format of the API response.
 */
- (void)request:(FBRequest *)request didLoad:(id)result {	
	XDKLog(@"request:didLoad");
}


/**
 * Called just before the request is sent to the server.
 */
- (void)requestLoading:(FBRequest *)request {	
	XDKLog(@"request:requestLoading");
}


/**
 * Called when the server responds and begins to send back data.
 */
- (void)request:(FBRequest *)request didReceiveResponse:(NSURLResponse *)response {
	XDKLog(@"request:didReceiveResponse");
}


/**
 * Called when a request returns a response.
 *
 * The result object is the raw response from the server of type NSData
 */
- (void)request:(FBRequest *)request didLoadRawResponse:(NSData *)data
{
	NSString* responseString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
	XDKLog(@"request:didLoadRawResponse for response: %@", responseString);
	
	// Valid JSON response data?
    NSError* error;
    if ([NSJSONSerialization JSONObjectWithData:data options:0 error:&error] != nil) {
        [self fireEvent:@"facebook.request.response"
                success:YES
             components:@{ @"raw": quotedString(responseString),
                           @"data": @"JSON.parse(e.raw)"}];
    }
    else {
        [self fireEvent:@"facebook.request.response"
                success:NO
             components:@{ @"raw": quotedString(responseString),
                           @"error": quotedString([error localizedDescription]) }];
    }
    _busy = NO;
}


#pragma mark - FBDialogDelegate methods


/**
 * Called when the dialog succeeds and is about to be dismissed.
 */
- (void)dialogDidComplete:(FBDialog *)dialog
{
	XDKLog(@"dialogDidComplete");
    _busy = NO;
}


/**
 * Called when the dialog succeeds with a returning url.
 */
- (void)dialogCompleteWithUrl:(NSURL *)url
{
	XDKLog(@"dialogCompleteWithUrl");
    [self reportDialogCompleteWithURL:url success:YES];
    _busy = NO;
}


/**
 * Called when the dialog get canceled by the user.
 */
- (void)dialogDidNotCompleteWithUrl:(NSURL *)url
{
	XDKLog(@"dialogDidNotCompleteWithUrl");
    [self reportDialogCompleteWithURL:url success:NO];
    _busy = NO;
}

- (void) reportDialogCompleteWithURL:(NSURL*)url success:(BOOL)success
{
	NSString* query = [url fragment];
    if (!query || query.length == 0) query = [url query];
    if (!query || query.length == 0) query = [url absoluteString];
    
    NSMutableDictionary* params = [NSMutableDictionary new];
    for (NSString* param in [query componentsSeparatedByString:@"&"]) {
        NSArray* kv = [param componentsSeparatedByString:@"="];
        params[kv[0]] = (kv.count == 1) ? @"" :
            [[kv[1] stringByReplacingOccurrencesOfString:@"+" withString:@" "]
                    stringByRemovingPercentEncoding];
    }
    
    NSMutableDictionary* extras = [NSMutableDictionary new];
    if (params[@"request"]) {
        extras[@"request"] = quotedString(params[@"request"]);
        NSMutableArray* toParms = [NSMutableArray new];
        for (int index = 0; ; ++index) {
            NSString* toIndex = [NSString stringWithFormat:@"to[%d]", index];
            if (!params[toIndex]) break;
            [toParms addObject:quotedString(params[toIndex])];
        }
        extras[@"to"] = [NSString stringWithFormat:@"[%@]",
                         [toParms componentsJoinedByString:@","]];
    }
    
    extras[@"raw"] = quotedString(query);
    extras[@"error"] = @"''";
    extras[@"data"] = @"{}";
    
    [self fireEvent:@"facebook.dialog.complete" success:success components:extras ];
}


/**
 * Called when the dialog is cancelled and is about to be dismissed.
 */
- (void)dialogDidNotComplete:(FBDialog *)dialog {
	XDKLog(@"%@", @"dialogDidNotComplete");
    _busy = NO;
}


/**
 * Called when dialog failed to load due to an error.
 */
- (void)dialog:(FBDialog*)dialog didFailWithError:(NSError *)error {
	XDKLog(@"didFailWithError");
    [self fireEvent:@"facebook.dialog.complete"
            success:NO
         components:@{ @"error": quotedString([error localizedDescription]),
                       @"raw": @"''",
                       @"data": @"{}" }];
    _busy = NO;
}


/**
 * Asks if a link touched by a user should be opened in an external browser.
 *
 * If a user touches a link, the default behavior is to open the link in the Safari browser,
 * which will cause your app to quit.  You may want to prevent this from happening, open the link
 * in your own internal browser, or perhaps warn the user that they are about to leave your app.
 * If so, implement this method on your delegate and return NO.  If you warn the user, you
 * should hold onto the URL and once you have received their acknowledgement open the URL yourself
 * using [[UIApplication sharedApplication] openURL:].
 */
- (BOOL)dialog:(FBDialog*)dialog shouldOpenURLInExternalBrowser:(NSURL *)url {
	XDKLog(@"dialog:shouldOpenURLInExternalBrowser: %@", url);
	[self fireEvent:@"facebook.link.click"
            success:YES
         components:@{ @"url": quotedString([url absoluteString]) }];

	return !_willHandleUserTouchingLink;
}


#pragma mark - Utility methods

//! Perform an action once there is an active Facebook session.
//!
//! If there is already an active session, then just execute the action. Otherwise,
//! do a login, saving the action to execute when the session is active.
//!
//! @note @a _busy is expected to be set on entry. It will be cleared by the login failure
//! delegate if the login fails, or when the action is complete if the login succeeds.
//!
//! @param eventName    An event name string to use in a failure event if the session login fails.
//! @param action       A block to execute if the session login succeeds.
//!
- (void) doWhenLoggedInEventName:(NSString*)eventName
                          action:(DeferredCommand)action
{
    if ([[FBDelegate sharedInstance].fb isSessionValid]) {
        action(self);
    }
    else {
        _toDoAfterLogin = action;
        _eventAfterLogin = eventName;
        [[FBDelegate sharedInstance] login:@[@"publish_stream",
                                             @"publish_actions",
                                             @"offline_access"]];
    }
}


//! Fire a JavaScript event.
//!
//! Generates a string of JavaScript code to create and dispatch an event.
//! @param eventName    The name of the event (not including the @c "intel.xdk." prefix).
//! @param success      The boolean value to assign to the @a success field in the
//!                     event object.
//! @param components   Each key/value pair in this dictionary will be incorporated.
//!                     (Note that the value must be a string which is the JavaScript
//!                     representation of the value - @c "true" for a boolean value,
//!                     @c "'Hello'" for a string, @c "20" for a number, etc.)
//!
- (void) fireEvent:(NSString*)eventName
           success:(BOOL)success
        components:(NSDictionary*)components
{
    NSMutableString* eventComponents = [NSMutableString string];
    for (NSString *eachKey in components) {
        [eventComponents appendFormat:@"e.%@ = %@;", eachKey, components[eachKey]];
    }
    NSString* script = [NSString stringWithFormat:@"var e = document.createEvent('Events');"
                        "e.initEvent('intel.xdk.%@', true, true);"
                        "e.success = %@;"
                        "%@"
                        "document.dispatchEvent(e);",
                        eventName,
                        (success ? @"true" : @"false"),
                        eventComponents];
    XDKLog(@"%@", script);
    [self.commandDelegate evalJs:script];
}


- (BOOL)isBusy
{
    if (_busy) {
        [self fireEvent:@"facebook.busy" success:NO components:@{ @"message": @"'busy'" }];
		return YES;
	}
	_busy = YES;
    return NO;
}


//! Turn a string into a Javascript string literal.
//!
//! Given an arbitrary string, get a string containing a Javascript string literal that
//! represents the input string. For example:
//!
//! -   <<abc>>         => <<"abc">>
//! -   <<"abc">>       => <<"\"abc\"">>
//! -   <<x=" \t\n\r">> => <<"x=\" \\t\\n\\t\"">>
//!
//! @remarks
//! The implementation relies on the Cocoa built-in JSON serialization code to do the
//! quoting. Since JSON can only represent arrays and objects, the code creates an array
//! containing the input string, gets its JSON representation, and then strips the array
//! literal square brackets from the beginning and end of the string.
//!
//! @param string   The string to be quoted.
//! @return         The string literal that represents @a string.
//!
static NSString* quotedString(NSString* string)
{
    NSError* err;
    NSData* jsonData = [NSJSONSerialization dataWithJSONObject:@[string] options:0 error:&err];
    NSMutableCharacterSet* trimChars = [NSMutableCharacterSet whitespaceAndNewlineCharacterSet];
    [trimChars addCharactersInString:@"[]"];
    NSString* jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    return [jsonString stringByTrimmingCharactersInSet:trimChars];
}

- (void)handleOpenURL:(NSNotification*)notification
{
    // override to handle urls sent to your app
    // register your url schemes in your App-Info.plist
	
    NSURL* url = [notification object];
	
    if ([url isKindOfClass:[NSURL class]]) {
		[[FBDelegate sharedInstance].fb handleOpenURL:url];
    }
}

@end
