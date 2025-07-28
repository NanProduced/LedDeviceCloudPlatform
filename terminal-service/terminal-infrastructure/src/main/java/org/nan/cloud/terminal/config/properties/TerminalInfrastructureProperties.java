package org.nan.cloud.terminal.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "terminal")
public class TerminalInfrastructureProperties {

    private Command command = new Command();

    @Data
    public static class Command {

        private Integer queue_timeout = 24 * 7;

        private Integer status_timeout = 24;
    }


}
