package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrgQuota {

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 最大存储空间（字节）
     */
    private Long maxStorageSize;

    /**
     * 已使用存储空间（字节）
     */
    private Long usedStorageSize;

    /**
     * 最大文件数量
     */
    private Integer maxFileCount;

    /**
     * 已使用文件数量
     */
    private Integer usedFileCount;

    /**
     * 告警阈值百分比
     */
    private Integer warningThresholdPercent;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
