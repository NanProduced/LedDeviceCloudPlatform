package org.nan.cloud.core.api.DTO.command.sub;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nan.cloud.core.api.DTO.command.BaseCommand;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("brightness")
@Schema(description = "亮度指令")
public class BrightnessCommand extends BaseCommand {

    @Schema(description = "亮度")
    private Integer brightness;

}
