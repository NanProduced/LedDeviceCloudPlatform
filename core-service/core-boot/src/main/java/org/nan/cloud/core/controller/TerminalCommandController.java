package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.command.sub.BrightnessCommand;
import org.nan.cloud.core.api.DTO.req.SingleCommandRequest;
import org.nan.cloud.core.api.DTO.res.SingleCommandResult;
import org.nan.cloud.core.api.TerminalCommandApi;
import org.nan.cloud.core.infrastructure.command.CommandHandler;
import org.nan.cloud.terminal.api.dto.command.SingleCommandSendResult;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TerminalCommand(终端指令控制器)", description = "封装指令操作")
@RequiredArgsConstructor
@RestController
public class TerminalCommandController implements TerminalCommandApi {

    private final CommandHandler commandHandler;

    @Operation(
            summary = "下发亮度指令",
            description = "下发指令调整终端亮度",
            tags = {"终端管理", "指令管理"}
    )
    @Override
    public SingleCommandResult brightnessCommand(SingleCommandRequest request) {
        BrightnessCommand command = (BrightnessCommand) request.getCommand();
        SingleCommandSendResult singleCommandSendResult = commandHandler.handleBrightnessCommand(request.getTid(), command.getBrightness());
        return new SingleCommandResult(singleCommandSendResult.isSuccess(), singleCommandSendResult.getTraceId());
    }
}
