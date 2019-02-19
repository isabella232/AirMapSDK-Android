# Migrating from verions 2.x to 3.x

AirMap is updating its identity provider and authentication mechanisms to be OIDC-compliant (https://openid.net/connect/). This will incur some breaking changes in version 3.0 of the AirMap SDK for Android. While these changes are minimal, there are a few things you will need to update if you are using any of the authentication features of the AirMap SDK.

### Configuration

The format of the `airmap.config.json` configuration file has changed slightly. Please visit the AirMap Developer Portal (https://dashboard.airmap.com/developer) to enter your application's package name if you use AirMap login & signup then download an updated configuration file.

### Refresh Tokens

The AirMapSDK no longer requires developers to manually refresh access tokens after they expire. The SDK now automatically handles refreshing access tokens so long as the refresh token has not been invalidated by the user. 

### Handling The Login Redirect

The new auth library uses a redirect uri that is dictated by a custom URL scheme that should be the same as your package name. Simple add this line to your build.gradle in the default config section:

```
manifestPlaceholders = ['appAuthRedirectScheme': applicationId]
```

# Getting Support

You can get support from AirMap via the following channels:
- Our developer workspace on [Slack](https://join.slack.com/t/airmap-developers/shared_invite/enQtNTA4MzU0MTM2MjI0LWYwYTM5MjUxNWNhZTQwYmYxODJmMjFiODAyNzZlZTRkOTY2MjUwMzQ1NThlZjczY2FjMDQ2YzgxZDcxNTY2ZGQ)
- Our developer guides and references at https://developers.airmap.com/