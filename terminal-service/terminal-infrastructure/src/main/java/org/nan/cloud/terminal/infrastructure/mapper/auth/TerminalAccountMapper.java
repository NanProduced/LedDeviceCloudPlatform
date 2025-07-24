package org.nan.cloud.terminal.infrastructure.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 终端设备账号Mapper
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Mapper
public interface TerminalAccountMapper extends BaseMapper<TerminalAccountEntity> {

    /**
     * 根据设备ID查询账号信息
     * 
     * @param deviceId 设备ID
     * @return 账号实体
     */
    @Select("SELECT * FROM terminal_account WHERE device_id = #{deviceId} AND deleted = 0")
    TerminalAccountEntity findByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 更新登录失败次数
     * 
     * @param deviceId 设备ID
     * @param failedAttempts 失败次数
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE terminal_account SET failed_attempts = #{failedAttempts}, " +
            "update_time = #{updateTime} WHERE device_id = #{deviceId} AND deleted = 0")
    int updateFailedAttempts(@Param("deviceId") String deviceId, 
                           @Param("failedAttempts") Integer failedAttempts,
                           @Param("updateTime") LocalDateTime updateTime);

    /**
     * 锁定账号
     * 
     * @param deviceId 设备ID
     * @param lockedTime 锁定时间
     * @param lockedUntil 锁定到期时间
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE terminal_account SET locked = 1, locked_time = #{lockedTime}, " +
            "locked_until = #{lockedUntil}, update_time = #{updateTime} " +
            "WHERE device_id = #{deviceId} AND deleted = 0")
    int lockAccount(@Param("deviceId") String deviceId,
                   @Param("lockedTime") LocalDateTime lockedTime,
                   @Param("lockedUntil") LocalDateTime lockedUntil,
                   @Param("updateTime") LocalDateTime updateTime);

    /**
     * 解锁账号
     * 
     * @param deviceId 设备ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE terminal_account SET locked = 0, failed_attempts = 0, " +
            "locked_time = NULL, locked_until = NULL, update_time = #{updateTime} " +
            "WHERE device_id = #{deviceId} AND deleted = 0")
    int unlockAccount(@Param("deviceId") String deviceId,
                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新最后登录信息
     * 
     * @param deviceId 设备ID
     * @param lastLoginTime 最后登录时间
     * @param lastLoginIp 最后登录IP
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE terminal_account SET failed_attempts = 0, " +
            "last_login_time = #{lastLoginTime}, last_login_ip = #{lastLoginIp}, " +
            "update_time = #{updateTime} WHERE device_id = #{deviceId} AND deleted = 0")
    int updateLastLogin(@Param("deviceId") String deviceId,
                       @Param("lastLoginTime") LocalDateTime lastLoginTime,
                       @Param("lastLoginIp") String lastLoginIp,
                       @Param("updateTime") LocalDateTime updateTime);

    /**
     * 查询需要解锁的账号
     * 
     * @param currentTime 当前时间
     * @return 待解锁账号列表
     */
    @Select("SELECT * FROM terminal_account WHERE locked = 1 AND locked_until <= #{currentTime} AND deleted = 0")
    List<TerminalAccountEntity> findAccountsToUnlock(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据组织ID查询账号列表
     * 
     * @param organizationId 组织ID
     * @return 账号列表
     */
    @Select("SELECT * FROM terminal_account WHERE organization_id = #{organizationId} AND deleted = 0")
    List<TerminalAccountEntity> findByOrganizationId(@Param("organizationId") String organizationId);

    /**
     * 统计在线设备数量
     * 
     * @param organizationId 组织ID
     * @param activeTime 活跃时间阈值
     * @return 在线设备数量
     */
    @Select("SELECT COUNT(*) FROM terminal_account WHERE organization_id = #{organizationId} " +
            "AND status = 'ACTIVE' AND last_login_time >= #{activeTime} AND deleted = 0")
    long countOnlineDevices(@Param("organizationId") String organizationId,
                           @Param("activeTime") LocalDateTime activeTime);
}