package org.nan.cloud.core.api.DTO.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.nan.cloud.core.api.DTO.command.sub.BrightnessCommand;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BrightnessCommand.class, name = "brightness")
})
@Data
public class BaseCommand {


    private String type;

}
