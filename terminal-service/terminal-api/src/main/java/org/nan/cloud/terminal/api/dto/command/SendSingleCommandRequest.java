package org.nan.cloud.terminal.api.dto.command;

import lombok.Data;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;

@Data
public class SendSingleCommandRequest {

    private Long tid;

    TerminalCommand command;
}
