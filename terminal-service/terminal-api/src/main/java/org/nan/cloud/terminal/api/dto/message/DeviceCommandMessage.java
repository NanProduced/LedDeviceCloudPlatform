package org.nan.cloud.terminal.api.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备指令消息DTO
 * 
 * 简化版本，只包含WebSocket连接必需的核心字段。
 * 业务字段由调用方补充。
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommandMessage {
    
    /**
     * 组织ID（用于区分终端）
     */
    private String oid;
    
    /**
     * 终端ID（用于区分终端）
     */
    private String did;
    
    /**
     * 消息内容（JSON格式，业务字段由调用方定义）
     */
    private String content;
}