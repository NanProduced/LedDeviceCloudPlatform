package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class User {

    private Long uid;

    private String username;

    private Long uGid;

    private Long oid;

    private String phone;

    private String email;

    /**
     * 0：system
     * 1：org manager
     * 2：normal
     */
    private Integer type;

    private LocalDateTime createTime;
}
