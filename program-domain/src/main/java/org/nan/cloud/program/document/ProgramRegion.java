package org.nan.cloud.program.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.nan.cloud.program.vsn.DisplayRect;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 节目区域(窗口) - 对应VSN <Region>
 * 每个页面可以包含多个区域，每个区域显示不同的内容
 */
@Data
public class ProgramRegion {
    
    /**
     * 窗口素材列表 - 对应VSN <Items>
     */
    @JsonProperty("items")
    @Field("items")
    private List<ProgramItem> items;
    
    /**
     * 节目窗口区域 - 对应VSN <Rect>
     * 建议加上，否则显示区域不是预期效果
     */
    @JsonProperty("rect")
    @Field("rect")
    private DisplayRect rect;
    
    /**
     * 节目窗口名称 - 对应VSN <name>
     * 设置节目类型：
     * "sync_program"：同步节目（item为2，3，6）
     * "singleline_scroll"：单行滚动节目（item为2或5，为5时需要IsScroll字段为1）
     */
    @JsonProperty("name")
    @Field("name")
    private String name;
    
    /**
     * 是否是排程区域 - 对应VSN <isScheduleRegion>
     * "0"=否, "1"=是（如果为1需要在Item中增加schedule）
     */
    @JsonProperty("isScheduleRegion")
    @Field("is_schedule_region")
    private String isScheduleRegion;
    
    /**
     * 区域层级 - 对应VSN <layer>
     * 存在多个Region时必需，Integer类型转字符串，值>0
     */
    @JsonProperty("layer")
    @Field("layer")
    private String layer;
}