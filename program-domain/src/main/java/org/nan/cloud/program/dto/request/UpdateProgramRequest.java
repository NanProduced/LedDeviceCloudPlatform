package org.nan.cloud.program.dto.request;

import lombok.Data;
import org.nan.cloud.program.enums.ProgramStatusEnum;


/**
 * 更新节目请求DTO
 */
@Data
public class UpdateProgramRequest {
    
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
     * 节目宽度（像素）
     */
    private Integer width;
    
    /**
     * 节目高度（像素）
     */
    private Integer height;
    
    /**
     * 节目状态
     */
    private ProgramStatusEnum status;
    
    /**
     * 节目缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 前端画布数据
     * 如果不为空，将更新节目内容并递增版本号
     */
    private Object canvasData;
}