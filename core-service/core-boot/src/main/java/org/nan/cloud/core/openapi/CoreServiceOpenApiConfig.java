package org.nan.cloud.core.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(title = "Core-Service-ApiDocs", version = "1.0", contact = @Contact(name = "Nan", email = "nanproduced@gamil.com")),
        servers = @Server(url = "http://192.168.1.222:8083"),
        security = @SecurityRequirement(name="Null")
)
public class CoreServiceOpenApiConfig {
}
