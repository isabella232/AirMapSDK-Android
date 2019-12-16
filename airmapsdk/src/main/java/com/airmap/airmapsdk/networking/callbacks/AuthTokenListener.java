package com.airmap.airmapsdk.networking.callbacks;

public interface AuthTokenListener {
    /**
     * The most current token can be obtained by calling AirMap.getAuthToken()
     */
    void onNewToken();
}
