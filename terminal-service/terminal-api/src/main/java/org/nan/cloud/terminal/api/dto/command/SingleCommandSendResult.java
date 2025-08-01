package org.nan.cloud.terminal.api.dto.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.nan.cloud.terminal.api.common.redis.ShareKeys;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class SingleCommandSendResult extends CommandSendResult{

    @Schema(description = "业务上追踪终端指令执行状态的ID")
    private String traceId;

    private Integer commandId;

    private Long tid;

    public static SingleCommandSendResult success(Long oid, Long tid, TerminalCommand command) {
        return SingleCommandSendResult.builder()
                .traceId(String.format(ShareKeys.TERMINAL_TRACE_ID_PATTERN, oid, tid, command.getId()))
                .commandId(command.getId())
                .tid(tid)
                .success(true)
                .authorUrl(command.getAuthorUrl())
                .uid(command.getUid())
                .build();
    }

    public static SingleCommandSendResult failed(Long oid, Long tid, TerminalCommand command) {
        return SingleCommandSendResult.builder()
                .traceId(String.format(ShareKeys.TERMINAL_TRACE_ID_PATTERN, oid, tid, command.getId()))
                .commandId(command.getId())
                .tid(tid)
                .success(false)
                .authorUrl(command.getAuthorUrl())
                .uid(command.getUid())
                .build();
    }
}
