package org.nan.cloud.auth.boot.config;

public class OAuth2Constants {

    public static final String SID = "sid";

    public static final class AUTHORIZATION_ATTRS {
        public static final String SESSION_ID = "session_id";
        public static final String LOGIN_STATE = "login_state";
        public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";
    }

    public static final class OIDC_PARAMETERS {
        public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    }
}
