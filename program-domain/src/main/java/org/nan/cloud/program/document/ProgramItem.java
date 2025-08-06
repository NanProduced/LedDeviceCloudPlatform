package org.nan.cloud.program.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.nan.cloud.program.vsn.*;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 节目项(素材) - 对应VSN <Item>
 * 每个区域包含的具体素材内容
 */
@Data
public class ProgramItem {
    
    /**
     * 节目类型 - 对应VSN <type>
     * 参考VsnItemTypeEnum中定义的类型
     */
    @JsonProperty("type")
    @Field("type")
    private String type;
    
    /**
     * 背景色 - 对应VSN <backcolor>
     * 8位十六进制整数转字符串（部分节目可省略）
     */
    @JsonProperty("backColor")
    @Field("back_color")
    private String backColor;
    
    /**
     * 入场特效 - 对应VSN <ineffect>
     * 图片和MultiPicInfo渲染文本有效果
     */
    @JsonProperty("inEffect")
    @Field("in_effect")
    private Effect inEffect;
    
    /**
     * 节目窗口素材名称 - 对应VSN <name>
     */
    @JsonProperty("name")
    @Field("name")
    private String name;
    
    /**
     * 节目排程 - 对应VSN <schedule>
     * isScheduleRegion为1时必需
     */
    @JsonProperty("schedule")
    @Field("schedule")
    private Schedule schedule;
    
    // === 图片/视频/GIF相关属性 ===
    
    /**
     * 文件源 - 对应VSN <filesource>
     * 图片/视频/GIF/音频等文件位置
     */
    @JsonProperty("fileSource")
    @Field("file_source")
    private FileSource fileSource;
    
    /**
     * 透明度 - 对应VSN <alpha>
     * 取值范围：[0,1] Float类型转字符串
     */
    @JsonProperty("alpha")
    @Field("alpha")
    private String alpha;
    
    /**
     * 素材播放时间 - 对应VSN <duration>
     * 单位：毫秒，Long类型转字符串
     */
    @JsonProperty("duration")
    @Field("duration")
    private String duration;
    
    /**
     * 是否缩放素材 - 对应VSN <reserveAS>
     * "1"=CENTER_INSIDE(否), "0"=FIT_XY(是)
     */
    @JsonProperty("reserveAS")
    @Field("reserve_as")
    private String reserveAS;
    
    /**
     * GIF播放次数 - 对应VSN <playTimes>
     * Integer类型转字符串，值>0
     */
    @JsonProperty("playTimes")
    @Field("play_times")
    private String playTimes;
    
    // === 文本相关属性 ===
    
    /**
     * 文本内容 - 对应VSN <Text>
     * 用于普通文本类型
     */
    @JsonProperty("text")
    @Field("text")
    private String text;
    
    /**
     * 文本颜色 - 对应VSN <TextColor>
     * 8位十六进制整数转字符串
     */
    @JsonProperty("textColor")
    @Field("text_color")
    private String textColor;
    
    /**
     * 字体样式 - 对应VSN <LogFont>
     */
    @JsonProperty("logFont")
    @Field("log_font")
    private LogFont logFont;
    
    /**
     * 字母间距 - 对应VSN <wordspacing>
     * Float类型转字符串，值>0.0
     */
    @JsonProperty("wordSpacing")
    @Field("word_spacing")
    private String wordSpacing;
    
    /**
     * 是否滚动 - 对应VSN <isscroll>
     * "0"=否, "1"=是
     */
    @JsonProperty("isScroll")
    @Field("is_scroll")
    private String isScroll;
    
    /**
     * 文本对齐方式 - 对应VSN <centeralalign>
     * "0"=居左, "1"=居中, "2"=居右
     */
    @JsonProperty("centerAlign")
    @Field("center_align")
    private String centerAlign;
    
    /**
     * 文本渐变色 - 对应VSN <textGradient>
     */
    @JsonProperty("textGradient")
    @Field("text_gradient")
    private TextGradient textGradient;
    
    // === 网页/流媒体相关属性 ===
    
    /**
     * URL地址 - 对应VSN <url>
     * 用于网页/流媒体类型
     */
    @JsonProperty("url")
    @Field("url")
    private String url;
    
    /**
     * 是否本地web素材 - 对应VSN <isLocal>
     * "0"=否, "1"=是
     */
    @JsonProperty("isLocal")
    @Field("is_local")
    private String isLocal;
    
    // === 传感器/环境信息相关属性 ===
    
    /**
     * 环境信息前缀 - 对应VSN <prevfix>
     * 如"温度: "（烟雾时没效果）
     */
    @JsonProperty("prefix")
    @Field("prefix")
    private String prefix;
    
    /**
     * 环境信息后缀 - 对应VSN <suffix>
     */
    @JsonProperty("suffix")
    @Field("suffix")
    private String suffix;
    
    // === 扩展属性 ===
    
    /**
     * 原始前端数据
     * 存储前端传来的原始JSON数据，用于后续处理和调试
     */
    @JsonProperty("rawData")
    @Field("raw_data")
    private Object rawData;
}