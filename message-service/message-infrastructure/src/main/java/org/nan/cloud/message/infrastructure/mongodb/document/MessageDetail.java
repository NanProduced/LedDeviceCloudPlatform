package org.nan.cloud.message.infrastructure.mongodb.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息详细内容文档
 * 
 * 存储消息的完整内容、附件信息和扩展数据。
 * 与MySQL中的MessageInfo形成关联，通过messageId进行关联查询。
 * 支持TTL自动过期，减少存储成本。
 * 
 * 业务场景：
 * - 消息内容的完整存储
 * - 富文本消息支持
 * - 消息附件管理
 * - 设备告警详细数据
 * - 任务结果详细信息
 * 
 * 索引设计：
 * - 单字段索引：messageId（唯一），organizationId
 * - 复合索引：organizationId + createdTime（按时间查询）
 * - TTL索引：ttl字段自动过期删除
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "message_details")
@CompoundIndexes({
    @CompoundIndex(name = "idx_org_created", def = "{'organizationId': 1, 'createdTime': -1}"),
    @CompoundIndex(name = "idx_receiver_created", def = "{'receiverId': 1, 'createdTime': -1}"),
    @CompoundIndex(name = "idx_type_created", def = "{'messageType': 1, 'createdTime': -1}")
})
public class MessageDetail {
    
    /**
     * MongoDB文档ID
     */
    @Id
    private String id;
    
    /**
     * 消息唯一标识
     * 与MySQL中的message_id字段对应
     */
    @Indexed(unique = true)
    @Field("messageId")
    private String messageId;
    
    /**
     * 组织ID
     * 用于多租户数据隔离和分片键
     */
    @Indexed
    @Field("organizationId")
    private String organizationId;
    
    /**
     * 消息类型
     * 便于按类型进行数据分析和查询优化
     */
    @Indexed
    @Field("messageType")
    private String messageType;
    
    /**
     * 发送者用户ID
     */
    @Field("senderId")
    private String senderId;
    
    /**
     * 发送者姓名
     * 冗余存储，减少关联查询
     */
    @Field("senderName")
    private String senderName;
    
    /**
     * 接收者用户ID
     */
    @Indexed
    @Field("receiverId")
    private String receiverId;
    
    /**
     * 接收者姓名
     * 冗余存储，减少关联查询
     */
    @Field("receiverName")
    private String receiverName;
    
    /**
     * 消息标题
     */
    @Field("title")
    private String title;
    
    /**
     * 消息正文内容
     * 支持纯文本和富文本格式
     */
    @Field("content")
    private String content;
    
    /**
     * 富文本内容
     * HTML格式的消息内容，用于支持格式化显示
     */
    @Field("richContent")
    private String richContent;
    
    /**
     * 消息扩展元数据
     * 
     * 存储特定类型消息的详细信息，如：
     * - 设备告警：deviceId, deviceName, alertType, currentValue, threshold等
     * - 任务结果：taskType, resultData, exportUrl等
     * - 系统通知：noticeType, effectiveTime, expireTime等
     */
    @Field("metadata")
    private Map<String, Object> metadata;
    
    /**
     * 消息附件列表
     */
    @Field("attachments")
    private List<MessageAttachment> attachments;
    
    /**
     * 消息标签
     * 用于消息分类和快速筛选
     */
    @Field("tags")
    private List<String> tags;
    
    /**
     * 消息优先级
     * 1-低，2-普通，3-高，4-紧急
     */
    @Field("priority")
    private Integer priority;
    
    /**
     * 消息来源
     * 标识消息的产生来源，如：system, user, device, task等
     */
    @Field("source")
    private String source;
    
    /**
     * 相关联的业务对象ID
     * 如设备ID、订单ID等，便于关联查询
     */
    @Field("relatedObjectId")
    private String relatedObjectId;
    
    /**
     * 相关联的业务对象类型
     * 如device、order、user等
     */
    @Field("relatedObjectType")
    private String relatedObjectType;
    
    /**
     * 消息创建时间
     */
    @Indexed
    @Field("createdTime")
    private LocalDateTime createdTime;
    
    /**
     * TTL过期时间
     * MongoDB会根据此字段自动删除过期文档
     * 默认30天后自动删除，可根据消息重要性调整
     */
    @Indexed(expireAfterSeconds = 0) // TTL索引，expireAfterSeconds=0表示使用文档中的时间
    @Field("ttl")
    private LocalDateTime ttl;
    
    /**
     * 消息附件内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageAttachment {
        
        /**
         * 附件文件名
         */
        private String fileName;
        
        /**
         * 附件文件URL
         * CDN或对象存储的访问地址
         */
        private String fileUrl;
        
        /**
         * 附件文件大小（字节）
         */
        private Long fileSize;
        
        /**
         * 附件MIME类型
         */
        private String mimeType;
        
        /**
         * 附件缩略图URL（图片类型）
         */
        private String thumbnailUrl;
        
        /**
         * 附件描述
         */
        private String description;
        
        /**
         * 上传时间
         */
        private LocalDateTime uploadTime;
    }
    
    /**
     * 获取格式化的消息内容
     * 
     * 优先返回富文本内容，如果没有则返回普通内容
     * 
     * @return 格式化内容
     */
    public String getFormattedContent() {
        return richContent != null && !richContent.trim().isEmpty() ? richContent : content;
    }
    
    /**
     * 检查消息是否包含附件
     * 
     * @return 是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
    
    /**
     * 获取附件总大小
     * 
     * @return 附件总大小（字节）
     */
    public Long getTotalAttachmentSize() {
        if (!hasAttachments()) {
            return 0L;
        }
        return attachments.stream()
                .mapToLong(attachment -> attachment.getFileSize() != null ? attachment.getFileSize() : 0L)
                .sum();
    }
    
    /**
     * 检查消息是否已过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        return ttl != null && ttl.isBefore(LocalDateTime.now());
    }
}