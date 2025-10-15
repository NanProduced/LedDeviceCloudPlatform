package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 显示矩形区域
 * 对应VSN XML中的Rect部分
 * 所有字段都是必填的，缺少任何字段将导致节目无法播放
 */
@Data
public class DisplayRect {
    
    /**
     * 起始X坐标 - 对应VSN <X>
     */
    @JsonProperty("x")
    @Field("x")
    private Integer x;
    
    /**
     * 起始Y坐标 - 对应VSN <Y>
     */
    @JsonProperty("y")
    @Field("y")
    private Integer y;
    
    /**
     * 矩形宽度 - 对应VSN <Width>
     */
    @JsonProperty("width")
    @Field("width")
    private Integer width;
    
    /**
     * 矩形高度 - 对应VSN <Height>
     */
    @JsonProperty("height")
    @Field("height")
    private Integer height;
    
    /**
     * 边框宽度 - 对应VSN <BorderWidth>
     */
    @JsonProperty("borderWidth")
    @Field("border_width")
    private Integer borderWidth;
    
    /**
     * 边框颜色 - 对应VSN <BorderColor>
     */
    @JsonProperty("borderColor")
    @Field("border_color")
    private String borderColor;
}