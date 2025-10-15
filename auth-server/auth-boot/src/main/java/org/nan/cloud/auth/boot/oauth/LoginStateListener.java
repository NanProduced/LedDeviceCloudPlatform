package org.nan.cloud.auth.boot.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.boot.config.OAuth2Constants;
import org.nan.cloud.auth.boot.oidc.enums.LoginStateEnum;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginStateListener implements ApplicationListener<SessionDestroyedEvent> {

    private final OidcAuthorizationService authorizationService;

    @Override
    public void onApplicationEvent(SessionDestroyedEvent event) {
        String sessionId = event.getId();
        authorizationService.findBySessionId(sessionId)
                .forEach(auth -> {
                    Integer loginState = auth.getAttribute(OAuth2Constants.AUTHORIZATION_ATTRS.LOGIN_STATE);
                    if (!LoginStateEnum.LOGOUT.getCode().equals(loginState)) {
                        OAuth2Authorization updated = OAuth2Authorization.from(auth)
                                .attribute(OAuth2Constants.AUTHORIZATION_ATTRS.LOGIN_STATE, LoginStateEnum.EXPIRED.getCode())
                                .build();
                        authorizationService.save(updated);
                        log.debug("Custom-Debug-log===>session_id: {} has expired, principalName: {}", sessionId, auth.getPrincipalName());
                    }
                });
    }
}
