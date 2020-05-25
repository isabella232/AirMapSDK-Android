package com.airmap.airmapsdk.networking.callbacks;

public interface AirMapSystemStatusListener {
    void onConnected();
    void onMessage(String component, String health);
    void onDisconnected();
}
