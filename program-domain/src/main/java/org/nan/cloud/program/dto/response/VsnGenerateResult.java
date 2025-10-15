package org.nan.cloud.program.dto.response;

import lombok.Data;

import java.util.List;

/**
 * VSN生成结果DTO
 */
@Data
public class VsnGenerateResult {
    
    /**
     * 是否生成成功
     */
    private Boolean success;
    
    /**
     * 生成的VSN XML内容
     */
    private String vsnXml;
    
    /**
     * 错误信息
     * 当success为false时提供详细错误信息
     */
    private String errorMessage;
    
    /**
     * 警告信息列表
     * 生成成功但有需要注意的问题
     */
    private List<String> warnings;
    
    /**
     * 使用的素材ID列表
     * 记录节目中引用的所有素材
     */
    private List<String> materialIds;
    
    /**
     * 节目总时长（毫秒）
     * 根据VSN内容计算得出
     */
    private Long totalDuration;
    
    /**
     * 创建成功的结果
     */
    public static VsnGenerateResult success(String vsnXml, Long totalDuration, List<String> materialIds) {
        VsnGenerateResult result = new VsnGenerateResult();
        result.setSuccess(true);
        result.setVsnXml(vsnXml);
        result.setTotalDuration(totalDuration);
        result.setMaterialIds(materialIds);
        return result;
    }
    
    /**
     * 创建失败的结果
     */
    public static VsnGenerateResult failure(String errorMessage) {
        VsnGenerateResult result = new VsnGenerateResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * 创建带警告的成功结果
     */
    public static VsnGenerateResult successWithWarnings(String vsnXml, Long totalDuration, List<String> materialIds, List<String> warnings) {
        VsnGenerateResult result = success(vsnXml, totalDuration, materialIds);
        result.setWarnings(warnings);
        return result;
    }
}