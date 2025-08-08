package org.nan.cloud.program.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 草稿发布请求DTO
 * 用于将草稿转换为正式节目，进入审核流程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishDraftRequest {
    
    /**
     * 可选：发布时修改的节目名称
     * 如果为空，则使用草稿的原始名称
     */
    private String name;
    
    /**
     * 可选：发布时修改的节目描述
     * 如果为空，则使用草稿的原始描述
     */
    private String description;
    
    /**
     * 发布说明
     * 记录此次发布的原因或变更说明
     */
    private String publishNote;
    
    /**
     * 可选：发布时更新的VSN数据
     * 如果为空，则使用草稿中的VSN数据
     */
    private String vsnData;
    
    /**
     * 可选：发布时更新的前端编辑器数据
     * 如果为空，则使用草稿中的contentData
     */
    private String contentData;
}