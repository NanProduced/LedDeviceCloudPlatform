package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.SingleCommandRequest;
import org.nan.cloud.core.api.DTO.res.SingleCommandResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TerminalCommandApi {

    String prefix = "/terminal_command";

    @PostMapping(prefix + "/brightness")
    SingleCommandResult brightnessCommand(@RequestBody SingleCommandRequest request);
}
