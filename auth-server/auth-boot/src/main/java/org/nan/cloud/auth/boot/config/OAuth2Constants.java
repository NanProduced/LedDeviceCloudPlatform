package org.nan.cloud.auth.boot.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OAuth2Constants {

    public static final String SID = "sid";

    public static final class AUTHORIZATION_ATTRS {
        public static final String SESSION_ID = "session_id";
        public static final String LOGIN_STATE = "login_state";
    }

    public static final class CLIENT_SETTINGS {
        public static final String BACKCHANNEL_REQUIRE = "settings.client.backchannel-logout-session-required";
        public static final String BACKCHANNEL_LOGOUT_URI = "settings.client.backchannel_logout_uri";
        public static final String POST_LOGOUT_REDIRECT_URI = "settings.client.post_logout_redirect_uri";
    }

    public static final class CLAIMS {
        public static final String SID = "sid";
        public static final String EVENTS = "events";
        public static final Map<String, Map<String, String>> EVENTS_VALUE = new HashMap<>();

        static {
            EVENTS_VALUE.put("http://schemas.openid.net/event/backchannel-logout", Collections.EMPTY_MAP);
        }
    }

    public static final class OIDC_PARAMETERS {
        public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    }
}
