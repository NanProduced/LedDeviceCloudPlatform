package org.nan.cloud.terminal.application.service.auth;

import org.nan.cloud.terminal.api.dto.auth.TerminalAuthDto;
import org.nan.cloud.terminal.api.dto.auth.TerminalLoginRequest;
import org.nan.cloud.terminal.api.dto.auth.TerminalLoginResponse;

/**
 * 终端设备认证服务接口
 * 
 * @author terminal-service
 * @since 1.0.0
 */
public interface TerminalAuthService {

    /**
     * 终端设备登录认证
     * 
     * @param request 登录请求
     * @param clientIp 客户端IP地址
     * @return 登录响应
     */
    TerminalLoginResponse login(TerminalLoginRequest request, String clientIp);

    /**
     * 验证终端设备认证令牌
     * 
     * @param token 认证令牌
     * @return 认证信息，如果令牌无效返回null
     */
    TerminalAuthDto validateToken(String token);

    /**
     * 终端设备登出
     * 
     * @param deviceId 设备ID
     * @return 是否成功
     */
    Boolean logout(String deviceId);

    /**
     * 刷新认证令牌
     * 
     * @param deviceId 设备ID
     * @return 新的认证令牌
     */
    String refreshToken(String deviceId);

    /**
     * 检查设备是否在线
     * 
     * @param deviceId 设备ID
     * @return 是否在线
     */
    Boolean isDeviceOnline(String deviceId);

    /**
     * 解锁过期的锁定账号
     * 
     * @return 解锁的账号数量
     */
    Integer unlockExpiredAccounts();

    /**
     * 获取设备认证信息
     * 
     * @param deviceId 设备ID
     * @return 认证信息
     */
    TerminalAuthDto getAuthInfo(String deviceId);

    /**
     * 强制下线设备
     * 
     * @param deviceId 设备ID
     * @param reason 下线原因
     * @return 是否成功
     */
    Boolean forceOffline(String deviceId, String reason);
}