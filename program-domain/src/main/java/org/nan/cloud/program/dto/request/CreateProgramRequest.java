package org.nan.cloud.program.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.program.enums.ProgramStatusEnum;



/**
 * 创建节目请求DTO
 * 支持前端传递的复杂数据结构，包括VSN生成数据和编辑器回显数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 节目时长（毫秒）
     * 前端计算得出的节目播放总时长
     */
    private Long duration;
    
    /**
     * 节目缩略图URL
     */
    private String thumbnailUrl;
    
    /**
     * VSN格式规范的JSON数据
     * 用于生成VSN文件，遵循VSN格式标准
     */
    private String vsnData;
    
    /**
     * 前端编辑器原始数据
     * 用于前端回显节目编辑状态，需要原样返回给前端
     * 存储前端编辑器的完整状态信息
     *
     * @return 前端编辑器原始数据

     */
    private String contentData;


}