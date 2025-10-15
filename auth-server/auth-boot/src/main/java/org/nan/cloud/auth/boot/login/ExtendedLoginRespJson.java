package org.nan.cloud.auth.boot.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExtendedLoginRespJson {

    /**
     * 认证结果响应码（200成功, 500失败）
     */
    private String code;
    /**
     * 响应结果描述信息
     */
    private String msg;
    /**
     * 认证成功后需前端对应的重定向Uri
     */
    private String redirectUri;

}
