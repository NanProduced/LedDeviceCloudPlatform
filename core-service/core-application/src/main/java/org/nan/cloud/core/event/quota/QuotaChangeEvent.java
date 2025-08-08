package org.nan.cloud.core.event.quota;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 组织存储空间改变事件
 * @author Nan
 */
public class QuotaChangeEvent extends ApplicationEvent {

    private Long oid;

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


    private QuotaChangeEventType type;



    public QuotaChangeEvent(Object source) {
        super(source);
    }

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


    /**
     * 存储空间改变事件类型
     */
    enum QuotaChangeEventType{

        /**
         * 素材上传
         */
        MATERIAL_FILE_UPLOAD,

        /**
         * 素材删除
         */
        MATERIAL_FILE_DELETE,

        /**
         * VSN创建
         */
        VSN_CREATE,

        /**
         * VSN删除
         */
        VSN_DELETE,

        /**
         * 导出文件生成
         */
        EXPORT_FILE_GENERATE,

        /**
         * 导出文件删除
         */
        EXPORT_FILE_DELETE

    }
}


