package org.nan.cloud.core.event.quota;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 组织存储空间改变事件
 * 简单事件对象：通过setter填充字段后发布
 * 注意：低并发场景，事件监听器内直接做结算与日志记录
 */
@Getter
public class QuotaChangeEvent extends ApplicationEvent {

    private QuotaChangeEventType  eventType;

    private String taskId;

    public QuotaChangeEvent(Object source, QuotaChangeEventType eventType, String taskId) {
        super(source);
        this.eventType = eventType;
        this.taskId = taskId;
    }

    /**
     * 事件类型
     */
    public enum QuotaChangeEventType {
        MATERIAL_FILE_UPLOAD,
        MATERIAL_FILE_DELETE,
        VSN_CREATE,
        VSN_DELETE,
        EXPORT_FILE_GENERATE,
        EXPORT_FILE_DELETE
    }
}

