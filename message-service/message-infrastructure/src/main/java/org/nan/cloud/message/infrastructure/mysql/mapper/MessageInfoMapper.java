package org.nan.cloud.message.infrastructure.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.message.infrastructure.mysql.entity.MessageInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息基础信息Mapper接口
 * 
 * 提供消息元数据的数据库访问功能，支持高效的消息查询、状态更新和统计分析。
 * 针对LED设备云平台的消息场景进行优化，支持多租户数据隔离。
 * 
 * 核心功能：
 * - 消息状态管理和批量更新
 * - 未读消息查询和统计
 * - 分页查询和条件筛选
 * - 消息过期处理
 * - 组织级消息统计
 * 
 * @author Nan
 * @since 1.0.0
 */
@Mapper
public interface MessageInfoMapper extends BaseMapper<MessageInfo> {
    
    /**
     * 分页查询用户未读消息
     * 
     * 按创建时间倒序排列，优先显示最新消息。
     * 支持组织级数据隔离，确保多租户安全。
     * 
     * @param page 分页参数
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param status 消息状态
     * @return 分页消息列表
     */
    @Select("SELECT * FROM message_info " +
            "WHERE receiver_id = #{receiverId} " +
            "AND organization_id = #{organizationId} " +
            "AND status = #{status} " +
            "AND (expires_at IS NULL OR expires_at > NOW()) " +
            "ORDER BY created_time DESC")
    IPage<MessageInfo> selectUnreadMessagesByPage(Page<MessageInfo> page,
                                                  @Param("receiverId") String receiverId,
                                                  @Param("organizationId") String organizationId,
                                                  @Param("status") String status);
    
    /**
     * 统计用户未读消息数量
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM message_info " +
            "WHERE receiver_id = #{receiverId} " +
            "AND organization_id = #{organizationId} " +
            "AND status IN ('PENDING', 'SENT', 'DELIVERED') " +
            "AND (expires_at IS NULL OR expires_at > NOW())")
    Long countUnreadMessages(@Param("receiverId") String receiverId,
                           @Param("organizationId") String organizationId);
    
    /**
     * 批量更新消息状态
     * 
     * 用于批量标记消息为已读、已送达等状态。
     * 支持按时间范围批量更新，提高处理效率。
     * 
     * @param messageIds 消息ID列表
     * @param newStatus 新状态
     * @param updateTime 更新时间戳
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE message_info SET " +
            "status = #{newStatus}, " +
            "updated_time = #{updateTime}, " +
            "<choose>" +
            "<when test='newStatus == \"SENT\"'>sent_time = #{updateTime}</when>" +
            "<when test='newStatus == \"DELIVERED\"'>delivered_time = #{updateTime}</when>" +
            "<when test='newStatus == \"READ\"'>read_time = #{updateTime}</when>" +
            "</choose>" +
            "WHERE message_id IN " +
            "<foreach collection='messageIds' item='messageId' open='(' separator=',' close=')'>" +
            "#{messageId}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("messageIds") List<String> messageIds,
                         @Param("newStatus") String newStatus,
                         @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 查询指定时间范围内的消息
     * 
     * 用于消息统计分析和数据导出功能。
     * 
     * @param organizationId 组织ID
     * @param messageType 消息类型（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    @Select("<script>" +
            "SELECT * FROM message_info " +
            "WHERE organization_id = #{organizationId} " +
            "AND created_time BETWEEN #{startTime} AND #{endTime} " +
            "<if test='messageType != null and messageType != \"\"'>" +
            "AND message_type = #{messageType} " +
            "</if>" +
            "ORDER BY created_time DESC" +
            "</script>")
    List<MessageInfo> selectByTimeRange(@Param("organizationId") String organizationId,
                                       @Param("messageType") String messageType,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 清理过期消息
     * 
     * 定时任务调用，清理已过期的消息记录。
     * 物理删除过期消息，释放存储空间。
     * 
     * @param beforeTime 过期时间点
     * @return 清理的消息数量
     */
    @Update("DELETE FROM message_info " +
            "WHERE expires_at IS NOT NULL " +
            "AND expires_at < #{beforeTime}")
    int deleteExpiredMessages(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 统计组织消息发送情况
     * 
     * 用于生成消息统计报表，分析消息发送效果。
     * 
     * @param organizationId 组织ID
     * @param messageType 消息类型
     * @param startTime 统计开始时间
     * @param endTime 统计结束时间
     * @return 统计结果Map，包含sent_count、delivered_count、read_count等
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "SUM(CASE WHEN status IN ('SENT', 'DELIVERED', 'READ') THEN 1 ELSE 0 END) as sent_count, " +
            "SUM(CASE WHEN status IN ('DELIVERED', 'READ') THEN 1 ELSE 0 END) as delivered_count, " +
            "SUM(CASE WHEN status = 'READ' THEN 1 ELSE 0 END) as read_count, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count " +
            "FROM message_info " +
            "WHERE organization_id = #{organizationId} " +
            "AND message_type = #{messageType} " +
            "AND created_time BETWEEN #{startTime} AND #{endTime}")
    java.util.Map<String, Long> getMessageStatistics(@Param("organizationId") String organizationId,
                                                     @Param("messageType") String messageType,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询用户最近的消息
     * 
     * 用于用户上线时推送最近的重要消息。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最近消息列表
     */
    @Select("SELECT * FROM message_info " +
            "WHERE receiver_id = #{receiverId} " +
            "AND organization_id = #{organizationId} " +
            "AND status IN ('PENDING', 'SENT', 'DELIVERED') " +
            "AND (expires_at IS NULL OR expires_at > NOW()) " +
            "ORDER BY created_time DESC " +
            "LIMIT #{limit}")
    List<MessageInfo> selectRecentUnreadMessages(@Param("receiverId") String receiverId,
                                                @Param("organizationId") String organizationId,
                                                @Param("limit") int limit);
}