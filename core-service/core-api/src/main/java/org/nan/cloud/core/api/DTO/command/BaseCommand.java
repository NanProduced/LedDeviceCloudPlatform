package org.nan.cloud.core.api.DTO.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.nan.cloud.core.api.DTO.command.sub.BrightnessCommand;
import org.nan.cloud.core.api.DTO.command.sub.ColorTempCommand;
import org.nan.cloud.core.api.DTO.command.sub.VolumeCommand;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BrightnessCommand.class, name = "brightness"),
        @JsonSubTypes.Type(value = ColorTempCommand.class, name = "colorTemp"),
        @JsonSubTypes.Type(value = VolumeCommand.class, name = "volume")
})
@Data
public class BaseCommand {


    private String type;

    /**
     * 可以实现相关的验证数据方法
     * @return
     */
    public boolean validate() {
        return true;
    }

}
