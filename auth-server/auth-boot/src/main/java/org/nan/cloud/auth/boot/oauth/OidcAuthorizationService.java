package org.nan.cloud.auth.boot.oauth;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

import java.util.List;

/**
 * 在OAuth2AuthorizationService服务接口基础上添加OIDC相关支持
 */
public interface OidcAuthorizationService extends OAuth2AuthorizationService {

    /**
     * 根据idToken查询认证信息
     * @param idToken
     * @return
     */
    OAuth2Authorization findByIdToken(String idToken);

    /**
     * 查询当前SessionId对应的已登录的认证信息
     * @param sessionId
     * @return
     */
    List<OAuth2Authorization> findBySessionId(String sessionId);

}
