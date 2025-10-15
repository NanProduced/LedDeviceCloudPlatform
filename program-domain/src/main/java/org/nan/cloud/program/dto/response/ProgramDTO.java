package org.nan.cloud.program.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目信息响应DTO
 */
@Data
public class ProgramDTO {
    
    /**
     * 节目ID
     */
    private Long id;
    
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
     * VSN生成状态
     */
    private VsnGenerationStatusEnum vsnGenerationStatus;
    
    /**
     * VSN生成状态显示名称
     */
    private String vsnGenerationStatusName;
    
    /**
     * VSN文件路径
     */
    private String vsnFilePath;
    
    /**
     * 最后发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPublishedTime;
    
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
    private Long createdBy;

    
    /**
     * 更新者用户ID
     */
    private Long updatedBy;
    
    /**
     * 用户组ID（用于权限层级控制）
     */
    private Long ugid;

}