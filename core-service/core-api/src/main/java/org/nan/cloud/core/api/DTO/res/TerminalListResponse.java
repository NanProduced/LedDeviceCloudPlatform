package org.nan.cloud.core.api.DTO.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "终端列表查询结果")
@Data
@Builder
public class TerminalListResponse {

    @Schema(description = "终端Id")
    private Long tid;

    @Schema(description = "终端名称")
    private String terminalName;

    @Schema(description = "终端描述")
    private String description;

    @Schema(description = "终端型号")
    private String terminalModel;

    @Schema(description = "终端组Id")
    private Long tgid;

    @Schema(description = "终端组名称")
    private String tgName;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "序列号")
    private String serialNumber;

    @Schema(description = "在线状态, 0:离线;1:在线")
    private Integer onlineStatus; // TODO: 实现终端在线状态查询

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "创建者")
    private Long createdBy;
}