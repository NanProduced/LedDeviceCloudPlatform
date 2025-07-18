package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchBindingOperationDTO {
    
    /**
     * 用户组ID
     */
    private Long ugid;
    
    /**
     * 要绑定的终端组ID列表
     */
    private List<Long> bindTgids;
    
    /**
     * 要解绑的终端组ID列表
     */
    private List<Long> unbindTgids;
    
    /**
     * 操作者ID
     */
    private Long operatorId;
    
    /**
     * 操作说明
     */
    private String operationDescription;
}