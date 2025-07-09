package org.nan.cloud.gateway.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(title = "CloudPlatformApiDocs", version = "1.0", contact = @Contact(name = "Nan", email = "nanproduced@gamil.com")),
    servers = @Server(url = "http://192.168.1.222:8082"),
    security = @SecurityRequirement(name="OAuth2/OIDC")
)
@Configuration
public class GatewayOpenApiConfig {
}
