package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息列表响应
 * 
 * 分页查询消息的响应结果，包含消息列表和分页信息。
 * 用于前端展示用户的消息列表，支持分页和筛选。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResponse {
    
    /**
     * 消息列表
     */
    private List<MessageDetailResponse> messages;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 未读消息数
     */
    private Long unreadCount;
    
    /**
     * 消息类型统计
     */
    private java.util.Map<String, Long> messageTypeStats;
    
    /**
     * 查询时间范围开始时间
     */
    private java.time.LocalDateTime startTime;
    
    /**
     * 查询时间范围结束时间
     */
    private java.time.LocalDateTime endTime;
}