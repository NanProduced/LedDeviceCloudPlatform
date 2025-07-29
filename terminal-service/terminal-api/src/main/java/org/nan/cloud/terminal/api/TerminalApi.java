package org.nan.cloud.terminal.api;

import org.nan.cloud.terminal.api.common.model.TerminalCommand;
import org.nan.cloud.terminal.api.common.model.TerminalCommandConfirm;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface TerminalApi {

    @PutMapping(value = "/wp-json/screen/v1/status", produces = "application/json;charset=UTF-8")
    void reportTerminalStatus(@RequestBody String report);

    @GetMapping("/wp-json/wp/v2/comments")
    List<TerminalCommand> getCommands(@RequestParam(value = "clt_type", defaultValue = "terminal") String clt_type,
                                      @RequestParam(value = "device_num") Integer device_num);

    @PostMapping("/wp-json/wp/v2/comments")
    void confirmCommand(@RequestParam("post") Integer post,
                        @RequestBody TerminalCommandConfirm commandConfirm);

}
