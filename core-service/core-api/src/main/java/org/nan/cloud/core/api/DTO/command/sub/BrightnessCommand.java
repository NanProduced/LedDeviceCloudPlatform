package org.nan.cloud.core.api.DTO.command.sub;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nan.cloud.core.api.DTO.command.BaseCommand;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("brightness")
public class BrightnessCommand extends BaseCommand {

    private Integer brightness;

}
