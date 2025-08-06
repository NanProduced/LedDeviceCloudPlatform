package org.nan.cloud.program.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 发布节目请求DTO
 */
@Data
public class PublishProgramRequest {
    
    /**
     * 节目ID
     */
    private String programId;
    
    /**
     * 目标终端组ID列表
     * 与terminalIds二选一，如果都提供，优先使用terminalGroupIds
     */
    private List<String> terminalGroupIds;
    
    /**
     * 目标终端ID列表
     * 与terminalGroupIds二选一
     */
    private List<String> terminalIds;
    
    /**
     * 是否立即发布
     * true=立即发布, false=定时发布（需要配合scheduledTime使用）
     */
    private Boolean immediate = true;
    
    /**
     * 定时发布时间
     * 当immediate为false时必须提供
     */
    private String scheduledTime;
    
    /**
     * 发布备注
     */
    private String comment;
}