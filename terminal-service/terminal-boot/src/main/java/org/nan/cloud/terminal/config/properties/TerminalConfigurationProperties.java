package org.nan.cloud.terminal.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        org.nan.cloud.terminal.config.properties.TerminalInfrastructureProperties.class
})
public class TerminalConfigurationProperties {
}
