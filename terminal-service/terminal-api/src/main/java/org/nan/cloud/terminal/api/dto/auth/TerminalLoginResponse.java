package org.nan.cloud.terminal.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端设备登录响应DTO
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Schema(description = "终端设备登录响应")
public class TerminalLoginResponse {

    @Schema(description = "认证结果", example = "true")
    private Boolean success;

    @Schema(description = "设备ID", example = "LED001")
    private String deviceId;

    @Schema(description = "设备名称", example = "大厅LED显示屏")
    private String deviceName;

    @Schema(description = "组织ID", example = "ORG001")
    private String organizationId;

    @Schema(description = "组织名称", example = "总部大厦")
    private String organizationName;

    @Schema(description = "认证令牌(Base64编码)", example = "TkVEMDAxOjEyMzQ1Ng==")
    private String token;

    @Schema(description = "令牌过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @Schema(description = "错误消息", example = "设备不存在或密码错误")
    private String errorMessage;

    @Schema(description = "剩余登录尝试次数", example = "4")
    private Integer remainingAttempts;

    @Schema(description = "锁定时间(如果账号被锁定)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedUntil;

    /**
     * 创建成功响应
     */
    public static TerminalLoginResponse success(String deviceId, String deviceName, 
                                              String organizationId, String organizationName,
                                              String token, LocalDateTime expireTime) {
        TerminalLoginResponse response = new TerminalLoginResponse();
        response.setSuccess(true);
        response.setDeviceId(deviceId);
        response.setDeviceName(deviceName);
        response.setOrganizationId(organizationId);
        response.setOrganizationName(organizationName);
        response.setToken(token);
        response.setExpireTime(expireTime);
        response.setLoginTime(LocalDateTime.now());
        return response;
    }

    /**
     * 创建失败响应
     */
    public static TerminalLoginResponse failure(String errorMessage, Integer remainingAttempts) {
        TerminalLoginResponse response = new TerminalLoginResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setRemainingAttempts(remainingAttempts);
        response.setLoginTime(LocalDateTime.now());
        return response;
    }

    /**
     * 创建锁定响应
     */
    public static TerminalLoginResponse locked(String errorMessage, LocalDateTime lockedUntil) {
        TerminalLoginResponse response = new TerminalLoginResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setRemainingAttempts(0);
        response.setLockedUntil(lockedUntil);
        response.setLoginTime(LocalDateTime.now());
        return response;
    }
}