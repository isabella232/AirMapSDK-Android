package com.airmap.airmapsdk.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import timber.log.Timber;

public class LoginActivity extends AppCompatActivity implements AuthorizationService.TokenResponseCallback {

    private AuthorizationService authService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        // parse response & exception
        Intent intent = getIntent();
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException exception = AuthorizationException.fromIntent(intent);

        // authorization completed
        if (response != null) {
            Timber.e("authorization completed: " + response.jsonSerializeString());

            // create authorization service to exchange token
            authService = new AuthorizationService(this);
            authService.performTokenRequest(response.createTokenExchangeRequest(), this);

        // authorization failed, check exception
        } else if (exception != null) {
            Timber.e("authorization failed: " + exception.toString());

            // if cancelled, just close activity
            if (exception.code == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.code) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (authService != null) {
            authService.dispose();
        }
    }

    @Override
    public void onTokenRequestCompleted(TokenResponse tokenResponse, AuthorizationException exception) {
        // exchange succeeded
        if (tokenResponse != null) {
            // send credentials via private broadcast back to callback
            sendBroadcast(tokenResponse);

            // close invisible activity
            finish();

        // authorization failed, check exception
        } else {
            if (exception != null) {
                Timber.e("Token request failed", exception);
            }

            //TODO: send failure via private broadcast back to callback?

            // close invisible activity
            finish();
        }
    }

    private void sendBroadcast(TokenResponse tokenResponse) {
        Intent intent = new Intent(AuthConstants.AUTHENTICATION_ACTION);
        intent.putExtra(AuthConstants.TOKEN_RESPONSE_EXTRA, tokenResponse.jsonSerializeString());

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
