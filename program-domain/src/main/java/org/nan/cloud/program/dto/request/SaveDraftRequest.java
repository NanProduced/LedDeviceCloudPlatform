package org.nan.cloud.program.dto.request;

import lombok.Data;

/**
 * 保存草稿请求DTO
 * 用于前端编辑器临时保存节目内容
 * 注意：验证逻辑应在Service层处理
 */
@Data
public class SaveDraftRequest {
    
    /**
     * 草稿名称（临时）
     * 必填字段
     */
    private String name;
    
    /**
     * 草稿描述
     */
    private String description;
    
    /**
     * 节目宽度（像素）
     * 必填字段
     */
    private Integer width;
    
    /**
     * 节目高度（像素）
     * 必填字段
     */
    private Integer height;
    
    /**
     * 前端画布数据
     * 存储前端编辑器的完整JSON数据
     * 必填字段
     */
    private Object canvasData;
    
    /**
     * 缩略图URL（可选）
     */
    private String thumbnailUrl;
}