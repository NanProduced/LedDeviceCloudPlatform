package org.nan.cloud.auth.boot.oidc.template.impl;

import org.nan.cloud.auth.boot.oidc.template.AbstractOidcTokenCustomer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class OidcTokenCustomerImpl extends AbstractOidcTokenCustomer {

    private Map<String, Set<String>> username2RolesMap = new HashMap<>(1);

    private Map<String, Consumer<JwtEncodingContext>> scope2ExtendFuncMap = new HashMap<>();

    @Override
    public String registerThirdUser(JwtEncodingContext jwtEncodingContext) {
        // todo:接入三方登录
        return  null;
    }

    @Override
    public void extendAccessToken(JwtEncodingContext jwtEncodingContext) {
        jwtEncodingContext.getAuthorizedScopes().stream()
                .filter(this.scope2ExtendFuncMap::containsKey)
                .forEach(scope -> {this.scope2ExtendFuncMap.get(scope).accept(jwtEncodingContext);});
    }

    private void extendRoles(JwtEncodingContext jwtEncodingContext) {
        String uid = jwtEncodingContext.getAuthorization().getPrincipalName();
        // todo: 用户名查角色
    }
}
