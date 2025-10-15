package org.nan.cloud.terminal.api.feign;

import org.nan.cloud.terminal.api.dto.command.SendSingleCommandRequest;
import org.nan.cloud.terminal.api.dto.command.SingleCommandSendResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 下发终端指令Feign RPC
 * @author Nan
 */
@FeignClient("terminal-service")
public interface TerminalCommandClient {

    String prefix = "/rpc/terminal/command";

    @PostMapping(prefix + "/send")
    SingleCommandSendResult sendCommand(@RequestBody SendSingleCommandRequest request);

}

