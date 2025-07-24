package org.nan.cloud.terminal.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端设备认证信息DTO
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Schema(description = "终端设备认证信息")
public class TerminalAuthDto {

    @Schema(description = "设备ID", example = "LED001")
    private String deviceId;

    @Schema(description = "设备名称", example = "大厅LED显示屏")
    private String deviceName;

    @Schema(description = "设备类型", example = "LED_SCREEN")
    private String deviceType;

    @Schema(description = "设备状态", example = "ONLINE")
    private String status;

    @Schema(description = "组织ID", example = "ORG001")
    private String organizationId;

    @Schema(description = "组织名称", example = "总部大厦")
    private String organizationName;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Schema(description = "登录失败次数", example = "0")
    private Integer failedAttempts;

    @Schema(description = "是否锁定", example = "false")
    private Boolean locked;

    @Schema(description = "锁定时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}