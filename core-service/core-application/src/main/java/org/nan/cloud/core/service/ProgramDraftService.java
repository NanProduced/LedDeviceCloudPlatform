package org.nan.cloud.core.service;

import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.SaveDraftRequest;
import org.nan.cloud.program.dto.request.UpdateDraftRequest;
import org.nan.cloud.program.dto.response.DraftDTO;
import org.nan.cloud.program.dto.response.ProgramDTO;

import java.util.List;
import java.util.Optional;

/**
 * 节目草稿管理服务接口
 * 处理草稿的特殊业务逻辑
 */
public interface ProgramDraftService {
    
    /**
     * 保存草稿
     * @param request 草稿保存请求
     * @param userId 创建者用户ID
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @return 保存的草稿信息
     */
    DraftDTO saveDraft(SaveDraftRequest request, Long userId, Long oid, Long ugid);
    
    /**
     * 更新草稿
     * @param draftId 草稿ID
     * @param request 更新请求
     * @param userId 更新者用户ID
     * @param oid 组织ID
     * @return 更新后的草稿信息
     */
    DraftDTO updateDraft(Long draftId, UpdateDraftRequest request, Long userId, Long oid);
    
    /**
     * 根据ID查询草稿详情
     * @param draftId 草稿ID
     * @param oid 组织ID
     * @return 草稿详情
     */
    Optional<DraftDTO> findDraftById(Long draftId, Long oid);
    
    /**
     * 分页查询用户组草稿列表
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 草稿列表
     */
    List<DraftDTO> findDraftsByUserGroup(Long oid, Long ugid, int page, int size);
    
    /**
     * 统计用户组草稿数量
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @return 草稿数量
     */
    long countDraftsByUserGroup(Long oid, Long ugid);
    
    /**
     * 分页查询用户创建的草稿列表
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 草稿列表
     */
    List<DraftDTO> findDraftsByCreator(Long oid, Long createdBy, int page, int size);
    
    /**
     * 统计用户创建的草稿数量
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @return 草稿数量
     */
    long countDraftsByCreator(Long oid, Long createdBy);
    
    /**
     * 删除草稿
     * @param draftId 草稿ID
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 是否删除成功
     */
    boolean deleteDraft(Long draftId, Long userId, Long oid);
    
    /**
     * 批量删除草稿
     * @param draftIds 草稿ID列表
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 删除的草稿数量
     */
    int deleteDraftsBatch(List<Long> draftIds, Long userId, Long oid);
    
    /**
     * 基于草稿创建正式节目
     * @param draftId 草稿ID
     * @param request 节目创建请求（可覆盖草稿中的部分信息）
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 创建的节目信息
     */
    ProgramDTO publishDraft(Long draftId, CreateProgramRequest request, Long userId, Long oid);
    
    /**
     * 检查草稿名称在用户组内是否可用
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param name 草稿名称
     * @param excludeId 排除的草稿ID（用于更新时检查）
     * @return 是否可用
     */
    boolean isDraftNameAvailable(Long oid, Long ugid, String name, Long excludeId);
    
    /**
     * 验证草稿数据完整性
     * @param draftId 草稿ID
     * @param oid 组织ID
     * @return 验证结果和错误信息
     */
    DraftValidationResult validateDraft(Long draftId, Long oid);
    
    /**
     * 检查用户是否有草稿访问权限
     * @param draftId 草稿ID
     * @param userId 用户ID
     * @param oid 用户所属组织ID
     * @return 是否有权限
     */
    boolean hasDraftAccessPermission(Long draftId, Long userId, Long oid);
    
    /**
     * 获取草稿的预览数据
     * @param draftId 草稿ID
     * @param oid 组织ID
     * @return 预览数据JSON
     */
    Optional<String> getDraftPreviewData(Long draftId, Long oid);
    
    /**
     * 自动保存草稿（用于前端定时保存）
     * @param draftId 草稿ID（可选，为null时创建新草稿）
     * @param request 保存请求
     * @param userId 用户ID
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @return 草稿信息
     */
    DraftDTO autoSaveDraft(Long draftId, SaveDraftRequest request, Long userId, Long oid, Long ugid);
    
    /**
     * 草稿验证结果
     */
    class DraftValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        public DraftValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
    }
}