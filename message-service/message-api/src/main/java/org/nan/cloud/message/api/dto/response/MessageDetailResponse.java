package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息详情响应
 * 
 * 包含消息的完整信息，结合MySQL基础数据和MongoDB详细内容。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDetailResponse {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息优先级
     */
    private Integer priority;
    
    /**
     * 发送者ID
     */
    private String senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 接收者ID
     */
    private String receiverId;
    
    /**
     * 接收者类型
     */
    private String receiverType;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 消息状态
     */
    private String status;
    
    /**
     * 是否已读
     */
    private Boolean isRead;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 消息元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 附件列表
     */
    private List<String> attachments;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;
}