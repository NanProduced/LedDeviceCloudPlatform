package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;

import java.time.LocalDateTime;

/**
 * 对业务中各种任务的封装
 * @author Nan
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Task {

    /**
     * 任务Id
     * UUID
     */
    private String taskId;

    private TaskTypeEnum taskType;

    private TaskStatusEnum taskStatus;

    private Long oid;

    /**
     * 关联的信息
     * 例如：
     * 素材上传 -> 上传的文件名
     * 文件导出 -> 导出的文件名
     * 素材转码 -> 转码的素材名
     */
    private String ref;

    /**
     * 关联到的业务Id - 使用String兼容自增Id和UUID
     * 素材上传 -> material_id
     * 文件导出 -> file_id
     * 素材转码 -> material_id
     */
    private String refId;

    /**
     * 文件导出任务的文件下载链接
     */
    private String downloadedUrl;

    /**
     * 素材相关任务的缩略图Url
     */
    private String thumbnailUrl;

    /**
     * 上传进度 (0-100)
     * 领域字段-不存到数据库
     */
    private Integer progress;

    private String errorMessage;

    /**
     * 任务元数据 -> MongoDB存储
     * 对应的Mongo文档的objectId
     */
    private String mataDataId;

    private Long creator;

    /**
     * 领域字段-不存到数据库
     */
    private String creatorName;

    private LocalDateTime createTime;

    private LocalDateTime completeTime;
}
