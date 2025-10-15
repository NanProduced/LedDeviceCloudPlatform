package org.nan.cloud.core.api.DTO.command.sub;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nan.cloud.core.api.DTO.command.BaseCommand;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("colorTemp")
@Schema(description = "色温指令")
public class ColorTempCommand extends BaseCommand {

    @Schema(description = "色温")
    private Integer colorTemp;
}
