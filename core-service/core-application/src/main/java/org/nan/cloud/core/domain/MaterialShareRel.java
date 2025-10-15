package org.nan.cloud.core.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialShareRel {

    /**
     * 主键Id
     */
    private Long shareId;

    /**
     * 资源id
     * 根据resourceType->fid或mid
     */
    private Long resourceId;

    /**
     * 1->素材文件
     * 2->素材文件夹
     */
    private Integer resourceType;

    /**
     * 共享到的用户组
     */
    private Long sharedTo;

    /**
     * 来自哪个用户组
     */
    private Long sharedFrom;

    private Long sharedBy;

    private LocalDateTime sharedTime;
}
