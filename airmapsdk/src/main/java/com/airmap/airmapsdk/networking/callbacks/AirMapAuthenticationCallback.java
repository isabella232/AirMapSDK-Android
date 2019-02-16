package com.airmap.airmapsdk.networking.callbacks;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.auth.AuthConstants;
import com.airmap.airmapsdk.models.pilot.AirMapPilot;
import com.airmap.airmapsdk.networking.services.AirMap;

import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import timber.log.Timber;

public class AirMapAuthenticationCallback {

    private Activity activity;
    private LoginCallback callback;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent data) {
            String action = data.getAction();
            if (action == null) {
                Timber.e("Received auth broadcast with no action set");
                return;
            }

            switch (action) {
                case AuthConstants.AUTHENTICATION_ACTION:
                    if (data.getExtras() != null && data.getExtras().containsKey(AuthConstants.ERROR_EXTRA)) {
                        onError(new Exception(data.getStringExtra(AuthConstants.ERROR_EXTRA)));
                    } else {
                        onEvent(Event.AUTHENTICATION, data);
                    }
                    break;
                case AuthConstants.SIGN_UP_ACTION:
                    onEvent(Event.SIGN_UP, data);
                    break;
                case AuthConstants.CANCELED_ACTION:
                    onEvent(Event.CANCELED, new Intent());
                    break;
                case AuthConstants.INVALID_CONFIG_ACTION:
                    onError(new Exception(data.getStringExtra(AuthConstants.ERROR_EXTRA)));
                    break;
            }
        }
    };

    public AirMapAuthenticationCallback(Activity activity, LoginCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AuthConstants.AUTHENTICATION_ACTION);
        filter.addAction(AuthConstants.SIGN_UP_ACTION);
        filter.addAction(AuthConstants.CANCELED_ACTION);
        filter.addAction(AuthConstants.INVALID_CONFIG_ACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
    }

    private void onAuthentication(String refreshToken, String accessToken) {
        // save refresh token to secure prefs
        AirMap.saveTokens(activity, accessToken, refreshToken);

        // set access token
        AirMap.setAuthToken(accessToken);

        // fetch pilot
        AirMap.getPilot(new AirMapCallback<AirMapPilot>() {
            @Override
            public void onSuccess(final AirMapPilot response) {
                Timber.d("get pilot success");
                callback.onSuccess(response);
            }

            @Override
            public void onError(final AirMapException e) {
                Timber.e(e, "get pilot failed");
                callback.onError(e);
            }
        });

        onDestroy();
    }

    private void onEvent(@Event int event, Intent data) {
        // parse event & data
        switch (event) {
            case Event.AUTHENTICATION:
                Timber.e("authentication");
                String tokenJSONSerialized = data.getStringExtra(AuthConstants.TOKEN_RESPONSE_EXTRA);
                try {
                    TokenResponse tokenResponse = TokenResponse.jsonDeserialize(tokenJSONSerialized);
                    onAuthentication(tokenResponse.refreshToken, tokenResponse.idToken);
                } catch (JSONException e) {
                    onError(new AirMapException(e.getMessage()));
                }
                break;

            case Event.CANCELED:
                Timber.e("canceled");
                onDestroy();
                break;

            case Event.RESET_PASSWORD:
                Timber.e("password reset");
                //TODO: ?
                break;

            case Event.SIGN_UP:
                Timber.e("signup");
                //TODO: ?
                break;
        }
    }

    private void onError(Exception error) {
        Timber.e(error, "Error authenticating with auth0");

        // bubble error to callback
        callback.onError(new AirMapException(error.getMessage()));

        // unregister receiver
        onDestroy();
    }

    public void onDestroy() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver);
    }

    @interface Event {
        int CANCELED = 0;
        int AUTHENTICATION = 1;
        int SIGN_UP = 2;
        int RESET_PASSWORD = 3;
    }
}
