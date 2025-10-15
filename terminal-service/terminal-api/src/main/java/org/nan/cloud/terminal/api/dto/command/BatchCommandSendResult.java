package org.nan.cloud.terminal.api.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class BatchCommandSendResult extends CommandSendResult{

    @Schema(description = "批量执行执行跟踪/聚合ID")
    private String batchId;

    private Map<Long, String> commandIdMap;

    @lombok.Builder.Default
    private CommandSendType sendType = CommandSendType.BATCH;

}
