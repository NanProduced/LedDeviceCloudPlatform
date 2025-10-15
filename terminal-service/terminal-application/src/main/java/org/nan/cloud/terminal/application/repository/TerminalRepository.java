package org.nan.cloud.terminal.application.repository;

import org.nan.cloud.terminal.application.domain.TerminalAccount;
import org.nan.cloud.terminal.application.domain.TerminalInfo;

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
public interface TerminalRepository {

    /**
     * 根据终端ID获取终端信息
     * 
     * @param tid 终端ID
     * @return 终端信息，不存在返回null
     */
    TerminalInfo getInfoByTid(Long tid);

    TerminalAccount getAccountByName(String accountName);

    void updateLastLogin(Long tid, String clientIp);

}