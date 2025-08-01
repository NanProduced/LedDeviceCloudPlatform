package org.nan.cloud.terminal.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
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

@Slf4j
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
        log.info("收到RPC指令下发请求: tid={}", request.getTid());
        
        // 从RPC调用上下文中获取用户信息
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        
        // 验证用户信息是否存在
        if (requestUser == null) {
            log.error("RPC调用缺少用户信息");
            throw new BaseException(ExceptionEnum.RPC_REQUEST_FAILED, "RPC调用缺少用户信息");
        }
        
        log.debug("RPC调用用户信息: uid={}, oid={}, ugid={}",
                requestUser.getUid(), requestUser.getOid(), requestUser.getUgid());

        // 设置用户id，以便message-service服务可以推送结果给下发指令的用户
        request.getCommand().setUid(requestUser.getUid());
        
        // 下发指令
        TerminalCommand sentCommand = terminalCommandManager.sendCommand(
                requestUser.getOid(), request.getTid(), request.getCommand());
        
        return SingleCommandSendResult.success(requestUser.getOid(), request.getTid(), sentCommand);
    }
}
