package org.nan.cloud.program.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 草稿详情响应DTO
 * 包含草稿的完整内容信息，用于编辑器回显
 */
@Data
public class DraftDetailDTO {
    
    /**
     * 草稿ID
     * 也就是节目ID
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
     * 前端画布数据
     * 包含完整的编辑器数据，用于恢复编辑状态
     */
    private Object canvasData;
    
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

    /* 草稿仅自己可见 没有修改者 */

    /**
     * 创建者ID
     */
    private Long createdBy;

}