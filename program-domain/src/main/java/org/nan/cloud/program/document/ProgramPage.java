package org.nan.cloud.program.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.nan.cloud.program.vsn.BgAudio;
import org.nan.cloud.program.vsn.BgFile;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 节目页 - 对应VSN <Page>
 * 每个节目可以包含多个页面
 */
@Data
public class ProgramPage {
    
    /**
     * 节目页区域列表 - 对应VSN <Regions>
     */
    @JsonProperty("regions")
    @Field("regions")
    private List<ProgramRegion> regions;
    
    /**
     * 播放时长方式 - 对应VSN <looptype>
     * "0"=指定播放时长, "1"=自动计算播放时长
     */
    @JsonProperty("loopType")
    @Field("loop_type")
    private String loopType;
    
    /**
     * 节目页时长 - 对应VSN <appointduration>
     * 单位：毫秒（需要设置looptype为0）
     */
    @JsonProperty("appointDuration")
    @Field("appoint_duration")
    private String appointDuration;
    
    /**
     * 节目页背景色 - 对应VSN <bgColor>
     * 8位16进制数
     */
    @JsonProperty("bgColor")
    @Field("bg_color")
    private Integer bgColor;
    
    /**
     * 背景文件 - 对应VSN <bgfile>
     * 对部分节目无效，如：流媒体
     */
    @JsonProperty("bgFile")
    @Field("bg_file")
    private BgFile bgFile;
    
    /**
     * 背景音频列表 - 对应VSN <bgaudios>
     */
    @JsonProperty("bgAudios")
    @Field("bg_audios")
    private List<BgAudio> bgAudios;
}