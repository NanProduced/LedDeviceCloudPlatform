package org.nan.cloud.core.infrastructure.repository.mongo.document;


import org.nan.cloud.core.enums.QuotaOperationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document("org_quota_change_log")
public class OrgQuotaChangeLogDocument {

    @Id
    private String objectId;

    private Long oid;

    /**
     * 可能多个素材指向同个文件
     * 内部字段，不暴露
     */
    private String fileId;

    /**
     * 上传的用户组（如果是组织级别资源则为null）
     */
    private Long ugid;

    /**
     * 上传到哪个文件夹
     * 组织级资源为null
     * 直接上传到用户组根目录为Null
     */
    private Long fid;


    /**
     * 文件的类型(业务类型,非实体资源类型)
     * MATERIAL:素材
     * VSN:节目VSN文件
     * EXPORT_FILE:导出文件
     * ...
     */
    private String fileType;

    /**
     * 文件的业务字段 - 由于不确定是自增ID还是UUID，使用string兼容
     * 类型为MATERIAL -> material_id
     * VSN -> vsnId
     * ...
     */
    private String sourceId;

    /**
     * 空间变化 (正加负减，单位字节）
     */
    private Long bytesChange;


    /**
     * 文件数量变化（+1/-1/0）
     */
    private Integer filesChange;

    /**
     * 操作类别
     */
    private QuotaOperationType quotaOperationType;

    private Long beforeUsedSize;

    private Long afterUsedSize;

    private Integer beforeUsedCount;

    private Integer afterUsedCount;

    /**
     * 操作者Id - 可能为null（系统操作）
     */
    private Long operationUid;

    /**
     * 对应的任务Id - 可能为空
     */
    private Long taskId;

    /**
     * 保留 - 备注、说明字段
     */
    private String remark;


    /**
     * 拓展字段
     */
    private Map<String, Object> extras;

    private LocalDateTime createdAt;
}
