package org.nan.cloud.program.dto.request;

import lombok.Data;

/**
 * 更新草稿请求DTO
 * 用于更新已存在的草稿内容
 * 注意：验证逻辑应在Service层处理
 */
@Data
public class UpdateDraftRequest {

    /**
     * 节目（草稿）Id
     */
    private Long programId;
    
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
     * 前端画布数据
     * 存储前端编辑器的完整JSON数据
     * 必填字段
     */
    private Object canvasData;
    
    /**
     * 缩略图URL
     */
    private String thumbnailUrl;
}