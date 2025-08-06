package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 文本渐变色配置 - 对应VSN <textGradient>
 * 用于设置文本的线性渐变效果
 */
@Data
public class TextGradient {
    
    /**
     * 渐变x开始坐标 - 对应VSN <gradientStartX>
     */
    @JsonProperty("gradientStartX")
    @Field("gradient_start_x")
    private String gradientStartX;
    
    /**
     * 渐变y开始坐标 - 对应VSN <gradientStartY>
     */
    @JsonProperty("gradientStartY")
    @Field("gradient_start_y")
    private String gradientStartY;
    
    /**
     * 渐变x结束坐标 - 对应VSN <gradientEndX>
     */
    @JsonProperty("gradientEndX")
    @Field("gradient_end_x")
    private String gradientEndX;
    
    /**
     * 渐变y结束坐标 - 对应VSN <gradientEndY>
     */
    @JsonProperty("gradientEndY")
    @Field("gradient_end_y")
    private String gradientEndY;
    
    /**
     * 渐变色列表 - 对应VSN <gradientColors>
     * 格式："0xffffbbaa,0xff6655aa"，多个颜色通过逗号分隔
     */
    @JsonProperty("gradientColors")
    @Field("gradient_colors")
    private String gradientColors;
    
    /**
     * 渐变色位置 - 对应VSN <gradientPositions>
     * 格式："0.0,0.9"，多个位置用逗号分隔
     * 取值范围：[0,1]
     */
    @JsonProperty("gradientPositions")
    @Field("gradient_positions")
    private String gradientPositions;
    
    /**
     * 渐变模型 - 对应VSN <gradientMode>
     * "0"=CLAMP, "1"=REPEAT, "2"=MIRROR
     */
    @JsonProperty("gradientMode")
    @Field("gradient_mode")
    private String gradientMode;
}