package org.nan.auth.boot.token;

import org.nan.auth.infrastructure.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtClaims implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getPrincipal() != null) {
            Authentication auth = context.getPrincipal();
            if (auth == null || auth.getPrincipal() == null) {
                return;
            }

            Object p = auth.getPrincipal();

            if (p instanceof UserPrincipal user) {
                context.getClaims()
                        .claim("uid", user.getUid())
                        .claim("oid", user.getOid())
                        .claim("uGid", user.getUGid());
            }
        }
    }
}
