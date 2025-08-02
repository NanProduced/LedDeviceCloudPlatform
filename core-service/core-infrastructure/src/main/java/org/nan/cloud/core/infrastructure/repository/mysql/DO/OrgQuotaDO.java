package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
     * 每日最大上传数量
     */
    @TableField("max_daily_uploads")
    private Integer maxDailyUploads;

    /**
     * 今日上传数量
     */
    @TableField("today_upload_count")
    private Integer todayUploadCount;

    /**
     * 上次重置日期
     */
    @TableField("last_reset_date")
    private LocalDate lastResetDate;

    /**
     * 告警阈值百分比
     */
    @TableField("warning_threshold_percent")
    private Integer warningThresholdPercent;

    /**
     * 是否启用配额限制
     */
    @TableField("is_quota_enabled")
    private Boolean quotaEnabled;

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

