package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 排程配置 - 对应VSN <schedule>
 * 用于节目排程时间控制（当isScheduleRegion为1时必需）
 */
@Data
public class Schedule {
    
    /**
     * 是否限制时间 - 对应VSN <isLimitTime>
     * "0"=否, "1"=是
     */
    @JsonProperty("isLimitTime")
    @Field("is_limit_time")
    private String isLimitTime;
    
    /**
     * 开始时间 - 对应VSN <startTime>
     * 格式：[00:00:00,23:59:59]
     */
    @JsonProperty("startTime")
    @Field("start_time")
    private String startTime;
    
    /**
     * 结束时间 - 对应VSN <endTime>
     * 格式：[00:00:00,23:59:59]
     */
    @JsonProperty("endTime")
    @Field("end_time")
    private String endTime;
    
    /**
     * 是否限制日期 - 对应VSN <isLimitDate>
     * "0"=否, "1"=是
     */
    @JsonProperty("isLimitDate")
    @Field("is_limit_date")
    private String isLimitDate;
    
    /**
     * 开始日期 - 对应VSN <startDay>
     * 格式：yyyy/MM/dd（如2023/08/14）
     */
    @JsonProperty("startDay")
    @Field("start_day")
    private String startDay;
    
    /**
     * 开始日期时间 - 对应VSN <startDayTime>
     * 格式：[00:00:00,23:59:59]
     */
    @JsonProperty("startDayTime")
    @Field("start_day_time")
    private String startDayTime;
    
    /**
     * 结束日期 - 对应VSN <endDay>
     * 格式：yyyy/MM/dd
     */
    @JsonProperty("endDay")
    @Field("end_day")
    private String endDay;
    
    /**
     * 结束日期时间 - 对应VSN <endDayTime>
     * 格式：[00:00:00,23:59:59]
     */
    @JsonProperty("endDayTime")
    @Field("end_day_time")
    private String endDayTime;
    
    /**
     * 是否限制星期 - 对应VSN <isLimitWeek>
     * "0"=否, "1"=是
     */
    @JsonProperty("isLimitWeek")
    @Field("is_limit_week")
    private String isLimitWeek;
    
    /**
     * 星期限制数组 - 对应VSN <limitWeek>
     * 格式："1,1,1,1,1,1,1" (0=关闭，1=播放)
     */
    @JsonProperty("limitWeek")
    @Field("limit_week")
    private String limitWeek;
}