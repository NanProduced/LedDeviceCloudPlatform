package org.nan.cloud.core.api.DTO.req;

import lombok.Data;
import org.nan.cloud.core.api.DTO.command.BaseCommand;

@Data
public class SingleCommandRequest {

    private Long tid;

    private BaseCommand command;
}
