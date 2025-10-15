package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 组织空间配额管理表对应的实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("organization_quota")
public class OrgQuotaDO {

    /**
     * 组织ID
     */
    @TableId("org_id")
    private Long orgId;

    /**
     * 最大存储空间（字节）
     */
    @TableField("max_storage_size")
    private Long maxStorageSize;

    /**
     * 已使用存储空间（字节）
     */
    @TableField("used_storage_size")
    private Long usedStorageSize;

    /**
     * 最大文件数量
     */
    @TableField("max_file_count")
    private Integer maxFileCount;

    /**
     * 已使用文件数量
     */
    @TableField("used_file_count")
    private Integer usedFileCount;

    /**
     * 告警阈值百分比
     */
    @TableField("warning_threshold_percent")
    private Integer warningThresholdPercent;

    /**
     * 创建时间
     */
    @TableField("created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField("updated_time")
    private LocalDateTime updatedTime;
}

