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

#import "FBDelegate.h"
#import "Facebook.h"
@implementation FBDelegate

static NSMutableArray* listeners = nil;
+ (void)addSessionListener:(id<FBSessionDelegate>)listener {
	if(listeners==nil) {
		listeners = [[NSMutableArray arrayWithCapacity:4] retain];
	}
	[listeners addObject:listener];
}

//note - it's possible to alloc/init instances of this class outside of the sharedInstance accessor but the class is not intended to be used except via the sharedInstance accessor

static FBDelegate* _instance = nil;
+ (id)sharedInstance 
{
	@synchronized(self) {
		if(_instance==nil) {
			NSString* fbAppId = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"FBAppId"];
			
			if(fbAppId!=nil) {
				_instance = [[FBDelegate alloc] init];
				
				//do init-type stuff here
				_instance->fbAppId = fbAppId;
				_instance->fb = [[Facebook alloc] initWithAppId:fbAppId andDelegate:_instance];
                
                NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
                _instance->fb.accessToken = [defaults objectForKey:@"FBAccessTokenKey"];
                _instance->fb.expirationDate = [defaults objectForKey:@"FBExpirationDateKey"];
			} else {
				NSLog(@"This app is attempting to access Facebook but has not been configured with a Facebook App Id.");
			}
		}
	}
	return _instance;
}

+ (BOOL)sharedInstanceIsNotNil
{
	return _instance!=nil;
}

@synthesize fb;

- (void) login:(NSArray*)permissions {
	[fb authorize:permissions];
}

- (void) logout {
	[fb logout];
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark FBSessionDelegate Methods:
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Called when the user successfully logged in.
 */
- (void)fbDidLogin {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[fb accessToken] forKey:@"FBAccessTokenKey"];
    [defaults setObject:[fb expirationDate] forKey:@"FBExpirationDateKey"];
    [defaults synchronize];
	
	for (id<FBSessionDelegate> listener in listeners) {
		[listener fbDidLogin];
	}
}

/**
 * Called when the user dismissed the dialog without logging in.
 */
- (void)fbDidNotLogin:(BOOL)cancelled {
	for (id<FBSessionDelegate> listener in listeners) {
		[listener fbDidNotLogin:cancelled];
	}
}

/**
 * Called after the access token was extended. If your application has any
 * references to the previous access token (for example, if your application
 * stores the previous access token in persistent storage), your application
 * should overwrite the old access token with the new one in this method.
 * See extendAccessToken for more details.
 */
- (void)fbDidExtendToken:(NSString*)accessToken expiresAt:(NSDate*)expiresAt {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[fb accessToken] forKey:@"FBAccessTokenKey"];
    [defaults setObject:[fb expirationDate] forKey:@"FBExpirationDateKey"];
    [defaults synchronize];

	for (id<FBSessionDelegate> listener in listeners) {
		[listener fbDidExtendToken:accessToken expiresAt:expiresAt];
	}
}

/**
 * Called when the user logged out.
 */
- (void)fbDidLogout {
	// Remove saved authorization information if it exists
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	if ([defaults objectForKey:@"FBAccessTokenKey"]) {
		[defaults removeObjectForKey:@"FBAccessTokenKey"];
		[defaults removeObjectForKey:@"FBExpirationDateKey"];
		[defaults synchronize];
	}
	
	for (id<FBSessionDelegate> listener in listeners) {
		[listener fbDidLogout];
	}	
}

/**
 * Called when the current session has expired. This might happen when:
 *  - the access token expired
 *  - the app has been disabled
 *  - the user revoked the app's permissions
 *  - the user changed his or her password
 */
- (void)fbSessionInvalidated {
	// Remove saved authorization information if it exists
	NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
	if ([defaults objectForKey:@"FBAccessTokenKey"]) {
		[defaults removeObjectForKey:@"FBAccessTokenKey"];
		[defaults removeObjectForKey:@"FBExpirationDateKey"];
		[defaults synchronize];
	}
	
	for (id<FBSessionDelegate> listener in listeners) {
		[listener fbSessionInvalidated];
	}
}

@end
