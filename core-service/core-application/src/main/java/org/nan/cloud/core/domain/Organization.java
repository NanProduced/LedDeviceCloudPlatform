package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Organization {

    private Long oid;

    private String name;

    private String remark;

    private Long rootUgid;

    private Long rootTgid;

    private Long creatorId;

    private Integer suffix;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
