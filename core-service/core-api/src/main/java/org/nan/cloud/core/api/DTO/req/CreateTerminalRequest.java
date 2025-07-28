package org.nan.cloud.core.api.DTO.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTerminalRequest {

    @NotBlank
    private String terminalName;

    private String description;

    @NotBlank
    private String terminalAccount;

    @NotBlank
    private String terminalPassword;

    @NotNull
    private Long tgid;
}
