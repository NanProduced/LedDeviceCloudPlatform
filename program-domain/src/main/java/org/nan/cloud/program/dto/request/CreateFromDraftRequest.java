package org.nan.cloud.program.dto.request;

import lombok.Data;

/**
 * 基于草稿创建正式节目请求DTO
 * 将草稿转换为正式节目的请求参数
 * 注意：验证逻辑应在Service层处理
 */
@Data
public class CreateFromDraftRequest {

    /**
     * 表明从哪个草稿创建
     * 草稿也算一种节目
     */
    private Long programId;
    
    /**
     * 正式节目名称
     * 必填字段
     */
    private String name;
    
    /**
     * 正式节目描述
     */
    private String description;
    
    /**
     * 是否删除原草稿
     * 默认为true，创建成功后删除草稿
     */
    private Boolean deleteDraft = true;
    
    /**
     * 是否立即提交审核
     * 如果组织启用审核，是否立即提交审核
     */
    private Boolean submitForApproval = false;
}