package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.command.sub.BrightnessCommand;
import org.nan.cloud.core.api.DTO.command.sub.ColorTempCommand;
import org.nan.cloud.core.api.DTO.command.sub.VolumeCommand;
import org.nan.cloud.core.api.DTO.req.SingleCommandRequest;
import org.nan.cloud.core.api.DTO.res.SingleCommandResult;
import org.nan.cloud.core.api.TerminalCommandApi;
import org.nan.cloud.core.infrastructure.command.CommandHandler;
import org.nan.cloud.terminal.api.dto.command.SingleCommandSendResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    public SingleCommandResult brightnessCommand(@RequestBody @Validated SingleCommandRequest request) {
        BrightnessCommand command = (BrightnessCommand) request.getCommand();
        SingleCommandSendResult singleCommandSendResult = commandHandler.handleBrightnessCommand(request.getTid(), command.getBrightness());
        return new SingleCommandResult(singleCommandSendResult.isSuccess(), singleCommandSendResult.getTraceId());
    }

    @Operation(
            summary = "下发色温指令",
            description = "下发指令调整设备色温",
            tags = {"终端管理", "指令管理"}
    )
    @Override
    public SingleCommandResult colorTempCommand(@RequestBody @Validated SingleCommandRequest request) {
        ColorTempCommand command = (ColorTempCommand) request.getCommand();
        SingleCommandSendResult singleCommandSendResult = commandHandler.handleColorTempCommand(request.getTid(), command.getColorTemp());
        return new SingleCommandResult(singleCommandSendResult.isSuccess(), singleCommandSendResult.getTraceId());
    }

    @Operation(
            summary = "下发音量指令",
            description = "下发指令调整设备音量",
            tags = {"终端管理", "指令管理"}
    )
    @Override
    public SingleCommandResult volumeCommand(@RequestBody @Validated SingleCommandRequest request) {
        VolumeCommand command = (VolumeCommand) request.getCommand();
        SingleCommandSendResult singleCommandSendResult = commandHandler.handleVolumeCommand(request.getTid(), command.getVolume());
        return new SingleCommandResult(singleCommandSendResult.isSuccess(), singleCommandSendResult.getTraceId());
    }

    @Operation(
            summary = "下发休眠指令",
            description = "下发指令使设备进入休眠状态",
            tags = {"终端管理", "指令管理"}
    )
    @Override
    public SingleCommandResult sleepCommand(@RequestParam("tid") Long tid) {
        SingleCommandSendResult singleCommandSendResult = commandHandler.handleSleepCommand(tid);
        return new SingleCommandResult(singleCommandSendResult.isSuccess(), singleCommandSendResult.getTraceId());
    }

    @Operation(
            summary = "下发唤醒指令",
            description = "下发指令使设备唤醒",
            tags = {"终端管理", "指令管理"}
    )
    @Override
    public SingleCommandResult wakeupCommand(@RequestParam("tid") Long tid) {
        SingleCommandSendResult singleCommandSendResult = commandHandler.handlerWakeupCommand(tid);
        return new SingleCommandResult(singleCommandSendResult.isSuccess(), singleCommandSendResult.getTraceId());
    }
}
