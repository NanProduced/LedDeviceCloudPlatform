package org.nan.cloud.program.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 节目信息 - 对应VSN <Information>
 * 存储节目的基础信息，如尺寸等
 */
@Data
public class ProgramInformation {
    
    /**
     * 节目宽度 - 对应VSN <Width>
     */
    @JsonProperty("width")
    @Field("width")
    private String width;
    
    /**
     * 节目高度 - 对应VSN <Height>
     */
    @JsonProperty("height")
    @Field("height")
    private String height;
}