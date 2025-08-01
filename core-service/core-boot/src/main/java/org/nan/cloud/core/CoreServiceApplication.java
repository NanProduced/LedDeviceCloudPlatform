package org.nan.cloud.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "org.nan.cloud")
@EnableFeignClients(basePackages = {
    "org.nan.cloud.auth.api.client",
    "org.nan.cloud.terminal.api.feign"
})
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class);
    }
}
