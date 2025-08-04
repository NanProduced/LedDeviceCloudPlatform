package org.nan.cloud.core.api.DTO.command.sub;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nan.cloud.core.api.DTO.command.BaseCommand;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("volume")
@Schema(description = "音量指令")
public class VolumeCommand extends BaseCommand {

    @Schema(description = "音量")
    private Integer volume;

}
