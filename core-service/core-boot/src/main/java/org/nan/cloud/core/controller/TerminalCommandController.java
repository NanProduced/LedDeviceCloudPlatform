package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.command.sub.BrightnessCommand;
import org.nan.cloud.core.api.DTO.req.SingleCommandRequest;
import org.nan.cloud.core.api.DTO.res.SingleCommandSendResult;
import org.nan.cloud.core.api.TerminalCommandApi;
import org.nan.cloud.core.infrastructure.command.CommandHandler;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TerminalCommand(终端指令控制器)", description = "封装指令操作")
@RequiredArgsConstructor
@RestController
public class TerminalCommandController implements TerminalCommandApi {

    private final CommandHandler commandHandler;

    @Override
    public SingleCommandSendResult brightnessCommand(SingleCommandRequest request) {
        BrightnessCommand command = (BrightnessCommand) request.getCommand();
        boolean success = commandHandler.handleBrightnessCommand(request.getTid(), command.getBrightness());
        if (success) {
            SingleCommandSendResult singleCommandSendResult = new SingleCommandSendResult();
            singleCommandSendResult.setSuccess(true);
        }
        return null;
    }
}
