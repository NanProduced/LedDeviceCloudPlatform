package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("task") // 设置数据库表名
public class TaskDO {

    /**
     * 任务Id
     * UUID
     */
    @TableId(value = "task_id", type = IdType.INPUT)  // 设置主键，类型为UUID
    private String taskId;

    @TableField("task_type")
    private TaskTypeEnum taskType;

    @TableField("task_status")
    private TaskStatusEnum taskStatus;

    @TableField("oid")
    private Long oid;

    /**
     * 关联的信息
     * 例如：
     * 素材上传 -> 上传的文件名
     * 文件导出 -> 导出的文件名
     * 素材转码 -> 转码的素材名
     */
    @TableField("ref")
    private String ref;

    /**
     * 关联到的业务Id - 使用String兼容自增Id和UUID
     * 素材上传 -> material_id
     * 文件导出 -> file_id
     * 素材转码 -> material_id
     */
    @TableField("ref_id")
    private String refId;

    /**
     * 文件导出任务的文件下载链接
     */
    @TableField("downloaded_url")
    private String downloadedUrl;

    /**
     * 素材相关任务的缩略图Url
     * todo: 还没填充
     */
    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField("error_message")
    private String errorMessage;

    /**
     * 任务元数据 -> MongoDB存储
     * 对应的Mongo文档的objectId
     */
    @TableField("mata_data_id")
    private String mataDataId;

    @TableField("creator")
    private Long creator;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("complete_time")
    private LocalDateTime completeTime;
}
