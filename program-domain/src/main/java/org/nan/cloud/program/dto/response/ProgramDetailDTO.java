package org.nan.cloud.program.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.nan.cloud.program.document.ProgramPage;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 节目详细信息响应DTO
 * 包含节目的完整内容信息
 */
@Data
public class ProgramDetailDTO {
    
    /**
     * 节目ID
     */
    private String id;
    
    /**
     * 节目名称
     */
    private String name;
    
    /**
     * 节目描述
     */
    private String description;
    
    /**
     * 节目状态
     */
    private ProgramStatusEnum status;
    
    /**
     * 节目状态显示名称
     */
    private String statusName;
    
    /**
     * 审核状态
     */
    private ProgramApprovalStatusEnum approvalStatus;
    
    /**
     * 审核状态显示名称
     */
    private String approvalStatusName;
    
    /**
     * 当前版本号
     */
    private Integer currentVersion;
    
    /**
     * 节目宽度（像素）
     */
    private Integer width;
    
    /**
     * 节目高度（像素）
     */
    private Integer height;
    
    /**
     * 节目时长（毫秒）
     */
    private Long duration;
    
    /**
     * 节目缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 使用次数
     */
    private Integer usageCount;
    
    /**
     * 最后发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPublishedTime;
    
    /**
     * 节目页列表
     * 包含节目的完整内容结构
     */
    private List<ProgramPage> pages;
    
    /**
     * 原始前端数据
     * 前端编辑器的原始JSON数据
     */
    private Object originalData;
    
    /**
     * 生成的VSN XML内容
     * 用于设备播放的VSN格式
     */
    private String vsnXml;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
    
    /**
     * 创建者用户ID
     */
    private String createdBy;
    
    /**
     * 创建者姓名
     */
    private String createdByName;
    
    /**
     * 更新者用户ID
     */
    private String updatedBy;
    
    /**
     * 更新者姓名
     */
    private String updatedByName;
}