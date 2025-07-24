package org.nan.cloud.terminal.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 终端设备登录请求DTO
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Schema(description = "终端设备登录请求")
public class TerminalLoginRequest {

    @Schema(description = "设备ID", example = "LED001", required = true)
    @NotBlank(message = "设备ID不能为空")
    @Size(min = 3, max = 50, message = "设备ID长度必须在3-50个字符之间")
    private String deviceId;

    @Schema(description = "设备密码", example = "123456", required = true)
    @NotBlank(message = "设备密码不能为空")
    @Size(min = 6, max = 100, message = "设备密码长度必须在6-100个字符之间")
    private String password;

    @Schema(description = "设备类型", example = "LED_SCREEN")
    private String deviceType;

    @Schema(description = "设备版本", example = "1.0.0")
    private String version;

    @Schema(description = "设备IP地址", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "设备MAC地址", example = "00:11:22:33:44:55")
    private String macAddress;
}