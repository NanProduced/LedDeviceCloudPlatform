package org.nan.cloud.terminal.infrastructure.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 终端设备账号Mapper
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Mapper
public interface TerminalAccountMapper extends BaseMapper<TerminalAccountDO> {

    /**
     * 根据设备ID查询账号信息
     * 
     * @param tid 设备ID
     * @return 账号实体
     */
    @Select("SELECT * FROM terminal_account WHERE tid = #{tid} AND deleted = 0")
    TerminalAccountDO findByTid(@Param("tid") Long tid);


    /**
     * 更新最后登录信息 - 第一次登录则记录first-login-time
     * 
     * @param tid 设备ID
     * @param lastLoginTime 最后登录时间
     * @param lastLoginIp 最后登录IP
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE terminal_account SET " +
            "last_login_time    = #{lastLoginTime}, " +
            "last_login_ip      = #{lastLoginIp}, " +
            "first_login_time   = COALESCE(first_login_time, #{lastLoginTime}), " +
            "update_time        = #{updateTime} " +
            "WHERE tid = #{tid} AND deleted = 0")
    int updateLastLogin(@Param("tid") Long tid,
                       @Param("lastLoginTime") LocalDateTime lastLoginTime,
                       @Param("lastLoginIp") String lastLoginIp,
                       @Param("updateTime") LocalDateTime updateTime);

    /**
     * 根据组织ID查询账号列表
     * 
     * @param oid 组织ID
     * @return 账号列表
     */
    @Select("SELECT * FROM terminal_account WHERE oid = #{oid} AND deleted = 0")
    List<TerminalAccountDO> findByOrganizationId(@Param("oid") Long oid);

}