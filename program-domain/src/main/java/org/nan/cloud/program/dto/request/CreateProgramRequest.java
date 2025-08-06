package org.nan.cloud.program.dto.request;

import lombok.Data;
import org.nan.cloud.program.enums.ProgramStatusEnum;


/**
 * 创建节目请求DTO
 */
@Data
public class CreateProgramRequest {
    
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
     * 默认为草稿状态
     */
    private ProgramStatusEnum status = ProgramStatusEnum.DRAFT;
    
    /**
     * 节目缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * 前端画布数据
     * 存储前端编辑器生成的完整JSON数据
     */
    private Object canvasData;
}