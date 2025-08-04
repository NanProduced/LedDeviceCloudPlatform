package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.SingleCommandRequest;
import org.nan.cloud.core.api.DTO.res.SingleCommandResult;
import org.springframework.web.bind.annotation.PostMapping;

public interface TerminalCommandApi {

    String prefix = "/terminal_command/single";

    @PostMapping(prefix + "/brightness")
    SingleCommandResult brightnessCommand(SingleCommandRequest request);

    @PostMapping(prefix + "/colorTemp")
    SingleCommandResult colorTempCommand(SingleCommandRequest request);

    @PostMapping(prefix + "/volume")
    SingleCommandResult volumeCommand(SingleCommandRequest request);

    @PostMapping(prefix + "/sleep")
    SingleCommandResult sleepCommand(Long tid);

    @PostMapping(prefix + "/wakeup")
    SingleCommandResult wakeupCommand(Long tid);
}
