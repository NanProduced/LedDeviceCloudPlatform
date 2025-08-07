package org.nan.cloud.program.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 素材验证结果DTO
 * 描述素材依赖验证的详细结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaterialValidationDTO {
    
    /**
     * 验证是否通过
     */
    private Boolean isValid;
    
    /**
     * 总素材数量
     */
    private Integer totalMaterials;
    
    /**
     * 有效素材数量
     */
    private Integer validMaterials;
    
    /**
     * 无效素材数量
     */
    private Integer invalidMaterials;
    
    /**
     * 缺失的素材ID列表
     */
    private List<Long> missingMaterialIds;
    
    /**
     * 验证错误信息列表
     */
    private List<String> errors;
    
    /**
     * 验证警告信息列表
     */
    private List<String> warnings;
    
    /**
     * 详细的素材验证结果
     */
    private List<MaterialItemValidation> materialDetails;
    
    /**
     * 单个素材验证结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MaterialItemValidation {
        
        /**
         * 素材ID
         */
        private Long materialId;
        
        /**
         * 素材名称
         */
        private String materialName;
        
        /**
         * 是否有效
         */
        private Boolean isValid;
        
        /**
         * 验证错误信息
         */
        private String errorMessage;
        
        /**
         * 素材当前状态
         */
        private String status;
        
        /**
         * 素材访问URL
         */
        private String accessUrl;
        
        /**
         * 素材MD5值
         */
        private String md5Hash;
    }
}