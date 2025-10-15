package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Role {

    private Long rid;

    private Long oid;

    private String name;

    private String displayName;

    private String description;

    /**
     * 0: system
     * 1: customer
     */
    private Integer roleType;

    private Long creatorId;

    private String creatorName;

    private LocalDateTime createTime;

    private Long updaterId;

    private String updaterName;

    private LocalDateTime updateTime;
}
