package org.nan.cloud.core.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排程模板类
 */
@Data
public class Schedule {

    /**
     * 排程Id
     */
    private String scheduleId;

    /**
     * 排程名称
     */
    private String scheduleName;

    /**
     * 所属组织
     */
    private Long oid;

    /**
     * 所属用户组
     */
    private Long ugid;

    /**
     * 创建者
     */
    private Long creatorId;

    private Long updaterId;

    /**
     * 排程数据 - 保存在mongoDB
     * 这里是mongoDB文档的objectId
     */
    private String scheduleDataId;

    /**
     * 关联的排程Id
     * 如果是由其他排程修改，绑定到初始排程
     */
    private String relatedSchedule;

    /**
     * 修改版本号
     * 从1开始++
     */
    private Integer version;

    /**
     * 是否为初始排程
     */
    private boolean sourceSchedule;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
