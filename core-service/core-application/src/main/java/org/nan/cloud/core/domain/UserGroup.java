package org.nan.cloud.core.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserGroup {

    private Long uGid;

    private String name;

    private Long oid;

    private Long parent;

    private String path;

    private String description;

    /**
     * 0：rog root
     * 1: normal
     */
    private Integer type;

    private Long creator;

    private LocalDateTime createTime;
}
