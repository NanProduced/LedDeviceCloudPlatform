package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.SingleCommandRequest;
import org.nan.cloud.core.api.DTO.res.SingleCommandSendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TerminalCommandApi {

    String prefix = "/terminal_command";

    @PostMapping(prefix + "/brightness")
    SingleCommandSendResult brightnessCommand(@RequestBody SingleCommandRequest request);
}
