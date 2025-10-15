package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 字体配置 - 对应VSN <logfont>
 * VSN格式中lfHeight是必填字段，缺少将导致解析失败
 */
@Data
public class LogFont {
    
    /**
     * 字体大小 - 对应VSN <lfHeight> (必填字段)
     */
    @JsonProperty("lfHeight")
    @Field("lf_height")
    private String lfHeight;
    
    /**
     * 字体加粗 - 对应VSN <lfWeight>
     * "400"=正常, "700"=加粗
     */
    @JsonProperty("lfWeight")
    @Field("lf_weight")
    private String lfWeight;
    
    /**
     * 字体倾斜 - 对应VSN <lfItalic>
     * "0"=否, "1"=是
     */
    @JsonProperty("lfItalic")
    @Field("lf_italic")
    private String lfItalic;
    
    /**
     * 使用下划线 - 对应VSN <lfUnderline>
     * "0"=否, "1"=是
     */
    @JsonProperty("lfUnderline")
    @Field("lf_underline")
    private String lfUnderline;
    
    /**
     * 字体名称 - 对应VSN <lfFaceName>
     */
    @JsonProperty("lfFaceName")
    @Field("lf_face_name")
    private String lfFaceName;
}