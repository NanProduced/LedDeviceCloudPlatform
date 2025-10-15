package org.nan.cloud.program.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存草稿请求DTO
 * 用于前端编辑器临时保存节目内容，与CreateProgramRequest保持字段一致
 * 注意：验证逻辑应在Service层处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDraftRequest {
    
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
     * -- GETTER --
     *  获取内容数据（兼容现有代码）
     *
     * @return 前端编辑器原始数据

     */
    private String contentData;
    
    // ===== 兼容性方法 =====

}