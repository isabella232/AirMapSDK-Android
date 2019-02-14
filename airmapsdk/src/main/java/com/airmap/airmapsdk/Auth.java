package com.airmap.airmapsdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.airmap.airmapsdk.auth.AuthConstants;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.networking.services.AuthService;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.airmap.airmapsdk.util.PreferenceUtils;
import com.airmap.airmapsdk.util.SecuredPreferenceException;
import com.auth0.android.jwt.JWT;

import java.util.Date;

import timber.log.Timber;

public class Auth {

    public static boolean isUserLoggedIn(Context context) {
        try {
            SharedPreferences preferences = PreferenceUtils.getPreferences(context);
            return preferences.getBoolean(AirMapConstants.LOGGED_IN, false);
        } catch (SecuredPreferenceException e) {
            Timber.e(e, "SecurePref");
            return false;
        }
    }

    /**
     * Refreshes the saved access token. Non-blocking
     */
    public static void refreshAccessToken(final Context context, final AirMapCallback<Void> callback) {
        Timber.v("Trying to refresh token");

        String refreshToken = null;
        try {
            SharedPreferences preferences = PreferenceUtils.getPreferences(context);
            refreshToken = preferences.getString(AuthConstants.REFRESH_TOKEN_KEY, "");
        } catch (SecuredPreferenceException e) {
            Timber.e(e, "Unable to get refresh token from secure prefs");
        }

        // return if refresh token is empty
        if (TextUtils.isEmpty(refreshToken)) {
            if (callback != null) {
                callback.error(new AirMapException("Invalid Refresh Token"));
            }
            return;
        }

        AuthService.refreshAccessToken(context, refreshToken, callback);
    }

    /**
     * Refresh Access Token. Blocking
     */
    public static void refreshAccessToken(final Context context) {
        Timber.v("Trying to refresh token");

        String refreshToken = null;
        try {
            SharedPreferences preferences = PreferenceUtils.getPreferences(context);
            refreshToken = preferences.getString(AuthConstants.REFRESH_TOKEN_KEY, "");
        } catch (SecuredPreferenceException e) {
            Timber.e(e, "Unable to get refresh token from secure prefs");
        }

        // return if refresh token is empty
        if (TextUtils.isEmpty(refreshToken)) {
            // throw ?
            return;
        }

        AuthService.refreshAccessToken(context, refreshToken);
    }

    public static void clearRefreshToken(final Context context) {
        try {
            SharedPreferences preferences = PreferenceUtils.getPreferences(context);
            preferences.edit()
                    .putString(AuthConstants.REFRESH_TOKEN_KEY, "")
                    .putString(AuthConstants.ACCESS_TOKEN_KEY, "")
                    .apply();
        } catch (SecuredPreferenceException e) {
            Timber.e(e, "Unable to get clear refresh token from secure prefs");
        }
    }

    public static boolean isTokenExpired() {
        String token = AirMap.getAuthToken();
        if (TextUtils.isEmpty(token)) {
            Timber.v("No auth token");
            return true;
        }

        JWT jwt = new JWT(token);
        Date expirationDate = jwt.getExpiresAt();
        return expirationDate == null || expirationDate.before(new Date());
    }
}
