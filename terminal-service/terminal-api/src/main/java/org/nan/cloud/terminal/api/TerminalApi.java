package org.nan.cloud.terminal.api;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TerminalApi {

    @PutMapping(value = "/wp-json/screen/v1/status", produces = "application/json;charset=UTF-8")
    void reportTerminalStatus(@RequestBody String report);


}
