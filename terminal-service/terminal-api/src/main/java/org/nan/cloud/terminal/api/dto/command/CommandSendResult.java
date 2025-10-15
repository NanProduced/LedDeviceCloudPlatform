package org.nan.cloud.terminal.api.dto.command;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class CommandSendResult {

    private Long uid;

    private String authorUrl;

    private boolean success;

    @lombok.Builder.Default
    private CommandSendType sendType = CommandSendType.SINGLE;

    public enum CommandSendType {

        SINGLE,

        BATCH;
    }


}
