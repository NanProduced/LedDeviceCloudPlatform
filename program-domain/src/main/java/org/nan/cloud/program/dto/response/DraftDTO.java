package org.nan.cloud.program.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 草稿响应DTO
 * 用于返回草稿的基本信息
 */
@Data
public class DraftDTO {
    
    /**
     * 草稿ID
     */
    private Long id;
    
    /**
     * 草稿名称
     */
    private String name;
    
    /**
     * 草稿描述
     */
    private String description;
    
    /**
     * 节目宽度（像素）
     */
    private Integer width;
    
    /**
     * 节目高度（像素）
     */
    private Integer height;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
    
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
     * 创建者姓名
     */
    private String createdByName;
    
    /**
     * 更新者用户ID
     */
    private Long updatedBy;
    
    /**
     * 更新者姓名
     */
    private String updatedByName;
}