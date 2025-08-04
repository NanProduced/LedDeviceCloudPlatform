package org.nan.cloud.core.api.DTO.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.nan.cloud.core.api.DTO.command.BaseCommand;

@Data
public class SingleCommandRequest {

    @NotNull
    private Long tid;

    private BaseCommand command;
}
