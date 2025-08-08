package org.nan.cloud.program.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节目内容响应DTO
 * 用于前端回显节目编辑器内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramContentDTO {
    
    /**
     * 节目ID
     */
    private Long programId;
    
    /**
     * 版本号
     */
    private Integer versionId;
    
    /**
     * 前端编辑器原始数据
     * 用于前端回显节目编辑状态
     */
    private String contentData;
    
    /**
     * VSN格式规范的JSON数据
     * 用于生成VSN文件
     */
    private String vsnData;
    
    /**
     * VSN XML数据（生成后的）
     */
    private String vsnXml;
    
    /**
     * 节目基础信息
     */
    private String name;
    private String description;
    private Integer width;
    private Integer height;
    private Long duration;
    private String thumbnailUrl;
}