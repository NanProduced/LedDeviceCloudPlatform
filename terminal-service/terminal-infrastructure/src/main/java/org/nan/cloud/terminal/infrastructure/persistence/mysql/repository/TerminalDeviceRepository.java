package org.nan.cloud.terminal.infrastructure.persistence.mysql.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalDeviceEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 终端设备MySQL数据访问层
 * 
 * 基于MyBatis Plus的高性能数据访问接口，提供：
 * 1. 基础CRUD操作 - 继承BaseMapper获得标准操作方法
 * 2. 设备认证查询 - 根据用户名密码进行设备认证
 * 3. 组织设备管理 - 按组织维度查询和管理设备
 * 4. 设备状态统计 - 在线设备数量、状态分布统计
 * 5. 性能优化查询 - 索引优化的分页查询和批量操作
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Mapper
public interface TerminalDeviceRepository extends BaseMapper<TerminalDeviceEntity> {

    /**
     * 根据设备ID查询设备信息
     * 用于设备连接时的快速身份验证
     * 
     * @param deviceId 设备ID
     * @return 设备实体，不存在返回null
     */
    @Select("SELECT * FROM terminal_device WHERE device_id = #{deviceId} AND deleted = 0")
    TerminalDeviceEntity findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据用户名查询设备信息
     * 用于Basic Auth认证时的用户名验证
     * 
     * @param username 设备认证用户名
     * @return 设备实体，不存在返回null
     */
    @Select("SELECT * FROM terminal_device WHERE username = #{username} AND deleted = 0")
    TerminalDeviceEntity findByUsername(@Param("username") String username);

    /**
     * 验证设备认证信息
     * 同时检查用户名和设备ID，确保认证安全性
     * 
     * @param username 设备认证用户名
     * @param deviceId 设备ID
     * @return 设备实体，认证失败返回null
     */
    @Select("SELECT * FROM terminal_device WHERE username = #{username} AND device_id = #{deviceId} " +
            "AND status = 'ACTIVE' AND deleted = 0")
    TerminalDeviceEntity findByUsernameAndDeviceId(@Param("username") String username, 
                                                   @Param("deviceId") String deviceId);

    /**
     * 查询组织下的所有设备
     * 支持分页查询，用于设备管理界面
     * 
     * @param organizationId 组织ID
     * @param page 分页参数
     * @return 分页设备列表
     */
    @Select("SELECT * FROM terminal_device WHERE organization_id = #{organizationId} AND deleted = 0 " +
            "ORDER BY created_at DESC")
    IPage<TerminalDeviceEntity> findByOrganizationId(@Param("organizationId") String organizationId, 
                                                     Page<TerminalDeviceEntity> page);

    /**
     * 查询指定状态的设备列表
     * 用于设备状态监控和管理
     * 
     * @param status 设备状态
     * @param page 分页参数
     * @return 分页设备列表
     */
    @Select("SELECT * FROM terminal_device WHERE status = #{status} AND deleted = 0 " +
            "ORDER BY last_online_time DESC")
    IPage<TerminalDeviceEntity> findByStatus(@Param("status") String status, 
                                           Page<TerminalDeviceEntity> page);

    /**
     * 查询在线设备列表
     * 基于最后在线时间判断设备是否在线（2分钟内有活动）
     * 
     * @param threshold 在线判断时间阈值
     * @return 在线设备列表
     */
    @Select("SELECT * FROM terminal_device WHERE status = 'ACTIVE' AND deleted = 0 " +
            "AND last_online_time >= #{threshold} ORDER BY last_online_time DESC")
    List<TerminalDeviceEntity> findOnlineDevices(@Param("threshold") LocalDateTime threshold);

    /**
     * 查询组织在线设备数量
     * 用于组织级别的设备统计
     * 
     * @param organizationId 组织ID
     * @param threshold 在线判断时间阈值
     * @return 在线设备数量
     */
    @Select("SELECT COUNT(*) FROM terminal_device WHERE organization_id = #{organizationId} " +
            "AND status = 'ACTIVE' AND deleted = 0 AND last_online_time >= #{threshold}")
    Integer countOnlineDevicesByOrganization(@Param("organizationId") String organizationId, 
                                           @Param("threshold") LocalDateTime threshold);

    /**
     * 统计各状态设备数量
     * 用于设备状态分布统计图表
     * 
     * @param organizationId 组织ID，为null时统计全部
     * @return 状态统计列表，包含status和count字段
     */
    @Select("<script>SELECT status, COUNT(*) as count FROM terminal_device " +
            "WHERE deleted = 0 " +
            "<if test='organizationId != null'>AND organization_id = #{organizationId}</if> " +
            "GROUP BY status</script>")
    List<java.util.Map<String, Object>> countDevicesByStatus(@Param("organizationId") String organizationId);

    /**
     * 批量更新设备最后在线时间
     * 用于批量更新在线状态，提升性能
     * 
     * @param deviceIds 设备ID列表
     * @param lastOnlineTime 最后在线时间
     * @return 更新影响的行数
     */
    @Update("<script>UPDATE terminal_device SET last_online_time = #{lastOnlineTime}, " +
            "updated_at = NOW() WHERE device_id IN " +
            "<foreach collection='deviceIds' item='deviceId' open='(' separator=',' close=')'>" +
            "#{deviceId}</foreach></script>")
    Integer batchUpdateLastOnlineTime(@Param("deviceIds") List<String> deviceIds, 
                                    @Param("lastOnlineTime") LocalDateTime lastOnlineTime);

    /**
     * 批量更新设备离线状态
     * 用于定时任务批量处理离线设备
     * 
     * @param deviceIds 设备ID列表
     * @param lastOfflineTime 最后离线时间
     * @return 更新影响的行数
     */
    @Update("<script>UPDATE terminal_device SET status = 'OFFLINE', " +
            "last_offline_time = #{lastOfflineTime}, updated_at = NOW() " +
            "WHERE device_id IN " +
            "<foreach collection='deviceIds' item='deviceId' open='(' separator=',' close=')'>" +
            "#{deviceId}</foreach></script>")
    Integer batchUpdateOfflineStatus(@Param("deviceIds") List<String> deviceIds, 
                                   @Param("lastOfflineTime") LocalDateTime lastOfflineTime);

    /**
     * 更新设备IP地址
     * 记录设备最后连接的IP地址
     * 
     * @param deviceId 设备ID
     * @param ipAddress IP地址
     * @return 更新影响的行数
     */
    @Update("UPDATE terminal_device SET ip_address = #{ipAddress}, updated_at = NOW() " +
            "WHERE device_id = #{deviceId}")
    Integer updateDeviceIpAddress(@Param("deviceId") String deviceId, 
                                @Param("ipAddress") String ipAddress);

    /**
     * 增加设备在线时长
     * 累计统计设备总在线时间
     * 
     * @param deviceId 设备ID
     * @param durationSeconds 本次在线时长（秒）
     * @return 更新影响的行数
     */
    @Update("UPDATE terminal_device SET " +
            "total_online_duration = COALESCE(total_online_duration, 0) + #{durationSeconds}, " +
            "updated_at = NOW() WHERE device_id = #{deviceId}")
    Integer addOnlineDuration(@Param("deviceId") String deviceId, 
                            @Param("durationSeconds") Long durationSeconds);

    /**
     * 查询长时间未上线的设备
     * 用于设备健康监控和告警
     * 
     * @param threshold 时间阈值
     * @param limit 查询数量限制
     * @return 长时间离线设备列表
     */
    @Select("SELECT * FROM terminal_device WHERE deleted = 0 " +
            "AND (last_online_time IS NULL OR last_online_time < #{threshold}) " +
            "ORDER BY last_online_time ASC LIMIT #{limit}")
    List<TerminalDeviceEntity> findLongOfflineDevices(@Param("threshold") LocalDateTime threshold, 
                                                     @Param("limit") Integer limit);

    /**
     * 根据设备分组查询设备
     * 用于分组管理和批量操作
     * 
     * @param deviceGroup 设备分组
     * @param organizationId 组织ID
     * @return 设备列表
     */
    @Select("SELECT * FROM terminal_device WHERE device_group = #{deviceGroup} " +
            "AND organization_id = #{organizationId} AND deleted = 0 " +
            "ORDER BY device_name ASC")
    List<TerminalDeviceEntity> findByDeviceGroup(@Param("deviceGroup") String deviceGroup, 
                                               @Param("organizationId") String organizationId);

    /**
     * 模糊搜索设备
     * 支持按设备名称、设备ID、序列号等字段搜索
     * 
     * @param keyword 搜索关键词
     * @param organizationId 组织ID
     * @param page 分页参数
     * @return 分页搜索结果
     */
    @Select("SELECT * FROM terminal_device WHERE deleted = 0 " +
            "AND organization_id = #{organizationId} " +
            "AND (device_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR device_id LIKE CONCAT('%', #{keyword}, '%') " +
            "OR serial_number LIKE CONCAT('%', #{keyword}, '%') " +
            "OR location LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY device_name ASC")
    IPage<TerminalDeviceEntity> searchDevices(@Param("keyword") String keyword, 
                                            @Param("organizationId") String organizationId, 
                                            Page<TerminalDeviceEntity> page);
}