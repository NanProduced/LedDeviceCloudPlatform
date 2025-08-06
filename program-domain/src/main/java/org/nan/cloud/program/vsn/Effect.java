package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 特效配置 - 对应VSN <ineffect>
 * 支持图片节目的入场特效
 */
@Data
public class Effect {
    
    /**
     * 特效类型 - 对应VSN <Type>
     * 具体枚举值参见VsnEffectTypeEnum
     */
    @JsonProperty("type")
    @Field("type")
    private String type;
    
    /**
     * 特效时长(毫秒) - 对应VSN <Time>
     */
    @JsonProperty("time")
    @Field("time")
    private String time;
}