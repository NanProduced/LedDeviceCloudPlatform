package org.nan.cloud.terminal.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.nan.cloud.terminal.api.dto.command.SendSingleCommandRequest;
import org.nan.cloud.terminal.api.dto.command.SingleCommandSendResult;
import org.nan.cloud.terminal.cache.TerminalCommandManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rpc/terminal/command")
public class RpcCommandController{

    private final TerminalCommandManager terminalCommandManager;

    @Operation(
            summary = "下发指令接口",
            description = "调用terminal-service下发指令到终端",
            tags = {"FEIGN_RPC"}
    )
    @PostMapping("/send")
    public SingleCommandSendResult sendCommand(@RequestBody SendSingleCommandRequest request) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        TerminalCommand sentCommand = terminalCommandManager.sendCommand(requestUser.getOid(), request.getTid(), request.getCommand());
        return SingleCommandSendResult.success(requestUser.getOid(), request.getTid(), sentCommand);
    }
}
