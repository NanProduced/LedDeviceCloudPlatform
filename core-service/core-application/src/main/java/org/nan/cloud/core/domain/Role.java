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

    /**
     * 0: system
     * 1: customer
     */
    private Integer type;

    private Long creatorId;

    private LocalDateTime createTime;
}
