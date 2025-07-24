package org.nan.cloud.terminal.api.dto.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 设备指令请求DTO
 * 
 * 用于设备向服务端查询待执行指令，对应WordPress REST API格式：
 * GET /wp-json/wp/v2/comments?clt_type=terminal
 * 
 * 请求参数：
 * - clt_type: 固定值"terminal"，用于区分终端设备请求
 * - device_id: 设备唯一标识，用于获取该设备的专属指令
 * - last_command_id: 最后执行的指令ID，用于增量获取
 * - max_commands: 最大返回指令数量，默认10条，最大50条
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class CommandRequestDTO {

    /**
     * 客户端类型，固定值"terminal"
     */
    @JsonProperty("clt_type")
    @NotBlank(message = "客户端类型不能为空")
    private String clientType = "terminal";

    /**
     * 设备ID
     */
    @JsonProperty("device_id")
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 最后执行的指令ID（用于增量获取）
     */
    @JsonProperty("last_command_id")
    private String lastCommandId;

    /**
     * 最大返回指令数量（默认10，最大50）
     */
    @JsonProperty("max_commands")
    private Integer maxCommands = 10;

    /**
     * 设备版本信息（可选）
     */
    @JsonProperty("device_version")
    private String deviceVersion;

    /**
     * 设备类型（可选）
     */
    @JsonProperty("device_type") 
    private String deviceType;
}