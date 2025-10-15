package org.nan.cloud.message.infrastructure.websocket.interceptor;

import lombok.Getter;
import org.nan.cloud.message.infrastructure.websocket.security.GatewayUserInfo;

import java.security.Principal;

/**
 * STOMP用户主体
 *
 * 封装用户认证信息，实现Principal接口
 */
@Getter
public class StompPrincipal implements Principal {

    private final GatewayUserInfo userInfo;

    public StompPrincipal(GatewayUserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String getName() {
        return userInfo.getUid().toString();
    }

}