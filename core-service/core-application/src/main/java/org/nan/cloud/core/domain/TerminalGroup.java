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
    
    private Long parentTgid; // 父终端组ID，与parent字段保持同步

    private String path;
    
    private Integer depth; // 层级深度

    private String description;

    /**
     * 0：org root
     * 1: normal
     */
    private Integer tgType;

    private Long creatorId;

    private LocalDateTime createTime;

    private Long updaterId;

    private LocalDateTime updateTime;
}
