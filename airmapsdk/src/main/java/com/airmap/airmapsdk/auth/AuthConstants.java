package com.airmap.airmapsdk.auth;

public class AuthConstants {

    // broadcast actions
    public static final String AUTHENTICATION_ACTION = "auth_action";
    public static final String SIGN_UP_ACTION = "sign_up_action";
    public static final String CANCELED_ACTION = "canceled_action";
    public static final String INVALID_CONFIG_ACTION = "invalid_config_action";

    // broadcast extras
    public static final String ERROR_EXTRA = "auth_error_extra";
    public static final String TOKEN_RESPONSE_EXTRA = "token_response_extra";

    // secure pref keys
    public static final String REFRESH_TOKEN_KEY = "AIRMAP_SDK_REFRESH_TOKEN";
    public static final String ACCESS_TOKEN_KEY = "AIRMAP_SDK_ACCESS_TOKEN";

    // keycloak params
    public static final String SCOPE = "openid email profile offline_access";
    public static final String REDIRECT_HOST = "oauth2";
    public static final String REDIRECT_PATH = "airmap";
}
