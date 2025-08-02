package org.nan.cloud.core.infrastructure.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.nan.cloud.terminal.api.dto.command.SendSingleCommandRequest;
import org.nan.cloud.terminal.api.dto.command.SingleCommandSendResult;
import org.nan.cloud.terminal.api.feign.TerminalCommandClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommandHandler {

    private final TerminalCommandClient terminalCommandClient;

    public SingleCommandSendResult handleBrightnessCommand(Long tid, Integer brightness) {
        SendSingleCommandRequest request = new SendSingleCommandRequest();
        TerminalCommand command = TerminalCommand.builder()
                .post(tid.intValue())
                .authorUrl("api/brightness")
                .karma(2)
                .content(new TerminalCommand.Content("{\"brightness\":" + brightness / 100 * 255 +"}"))
                .build();
        request.setTid(tid);
        request.setCommand(command);
        return terminalCommandClient.sendCommand(request);
    }

}
