package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TerminalGroup {

    private Long tgid;

    private String name;

    private Long oid;

    private Long parent;

    private String path;

    private String description;

    /**
     * 0：org root
     * 1: normal
     */
    private Integer type;

    private Long creatorId;

    private LocalDateTime createTime;
}
