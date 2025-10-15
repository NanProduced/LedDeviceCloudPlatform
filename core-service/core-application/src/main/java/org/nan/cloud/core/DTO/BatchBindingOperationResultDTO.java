package org.nan.cloud.core.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchBindingOperationResultDTO {
    
    /**
     * 操作是否成功
     */
    private Boolean success;
    
    /**
     * 操作消息
     */
    private String message;
    
    /**
     * 实际创建的绑定数量
     */
    private Integer createdBindings;
    
    /**
     * 实际删除的绑定数量
     */
    private Integer deletedBindings;
    
    /**
     * 操作详情
     */
    private List<BindingOperationDetailDTO> operationDetails;
    
    @Data
    @Builder
    public static class BindingOperationDetailDTO {
        private Long tgid;
        private String terminalGroupName;
        private String operationType;
        private String description;
    }
}