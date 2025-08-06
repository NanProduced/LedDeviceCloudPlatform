package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 背景音频配置 - 对应VSN <bgaudios>
 * 用于节目页背景音频设置
 */
@Data
public class BgAudio {
    
    /**
     * 音频文件源 - 对应VSN <filesource>
     */
    @JsonProperty("fileSource")
    @Field("file_source")
    private FileSource fileSource;
    
    /**
     * 音频音量 - 对应VSN <volume>
     * 取值范围：0.0~1.0
     */
    @JsonProperty("volume")
    @Field("volume")
    private String volume;
}