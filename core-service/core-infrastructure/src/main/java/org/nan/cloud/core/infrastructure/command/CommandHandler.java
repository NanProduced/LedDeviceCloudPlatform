package org.nan.cloud.core.infrastructure.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
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
        TerminalCommand command = TerminalCommand.brightnessCommand(tid, brightness);
        request.setTid(tid);
        request.setCommand(command);
        return terminalCommandClient.sendCommand(request);
    }

    public SingleCommandSendResult handleColorTempCommand(Long tid, Integer colorTemp) {
        SendSingleCommandRequest request = new SendSingleCommandRequest();
        TerminalCommand command = TerminalCommand.colorTempCommand(tid, colorTemp);
        request.setTid(tid);
        request.setCommand(command);
        return terminalCommandClient.sendCommand(request);
    }

    public SingleCommandSendResult handleVolumeCommand(Long tid, Integer volume) {
        SendSingleCommandRequest request = new SendSingleCommandRequest();
        TerminalCommand command = TerminalCommand.volumeCommand(tid, volume);
        request.setTid(tid);
        request.setCommand(command);
        return terminalCommandClient.sendCommand(request);
    }

    public SingleCommandSendResult handleSleepCommand(Long tid) {
        SendSingleCommandRequest request = new SendSingleCommandRequest();
        request.setTid(tid);
        request.setCommand(TerminalCommand.sleepCommand(tid));
        return terminalCommandClient.sendCommand(request);
    }

    public SingleCommandSendResult handlerWakeupCommand(Long tid) {
        SendSingleCommandRequest request = new SendSingleCommandRequest();
        request.setTid(tid);
        request.setCommand(TerminalCommand.wakeupCommand(tid));
        return terminalCommandClient.sendCommand(request);
    }

}
