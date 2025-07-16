package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class User {

    private Long uid;

    private String username;

    private String password;

    private Long ugid;

    private String ugName;

    private Long oid;

    private String phone;

    private String email;

    /**
     * 0：enabled
     * 1：block
     */
    private Integer status;

    /**
     * 0：system
     * 1：org manager
     * 2：normal
     */
    private Integer userType;

    private Integer suffix;

    private Long creatorId;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}
