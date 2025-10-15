package org.nan.cloud.terminal.websocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.terminal.api.common.model.TerminalCommand;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebsocketTerminalCommand {

    private TerminalCommand data;

    /**
     * 播放盒设备仅支持Integer
     */
    @JsonProperty("led_id")
    private Integer tid;
}
