package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;

import java.util.List;
import java.util.Optional;

/**
 * 节目Repository接口
 * 处理节目的MySQL数据访问
 */
public interface ProgramRepository {
    
    /**
     * 根据ID查询节目
     * @param programId 节目ID
     * @return 节目信息
     */
    Optional<Program> findById(Long programId);
    
    /**
     * 根据ID和组织查询节目（权限控制）
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 节目信息
     */
    Optional<Program> findByIdAndOid(Long programId, Long oid);
    
    /**
     * 查询用户组下的节目列表
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 节目列表
     */
    List<Program> findByUserGroup(Long oid, Long ugid, ProgramStatusEnum status, int page, int size);
    
    /**
     * 分页查询节目列表（支持关键词搜索）
     * 使用MyBatis Plus分页插件
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param keyword 搜索关键词（可选）
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return MyBatis Plus分页结果
     */
    PageVO<Program> findProgramsPage(Long oid, Long ugid, String keyword, ProgramStatusEnum status, int page, int size);
    
    /**
     * 统计用户组下的节目数量
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param status 节目状态（可选）
     * @return 节目数量
     */
    long countByUserGroup(Long oid, Long ugid, ProgramStatusEnum status);
    
    /**
     * 查询用户创建的节目列表
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 节目列表
     */
    List<Program> findByCreator(Long oid, Long createdBy, ProgramStatusEnum status, int page, int size);
    
    /**
     * 统计用户创建的节目数量
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @param status 节目状态（可选）
     * @return 节目数量
     */
    long countByCreator(Long oid, Long createdBy, ProgramStatusEnum status);
    
    /**
     * 查询组织下的节目列表（管理员视图）
     * @param oid 组织ID
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 节目列表
     */
    List<Program> findByOrganization(Long oid, ProgramStatusEnum status, int page, int size);
    
    /**
     * 统计组织下的节目数量
     * @param oid 组织ID
     * @param status 节目状态（可选）
     * @return 节目数量
     */
    long countByOrganization(Long oid, ProgramStatusEnum status);
    
    /**
     * 检查节目名称是否在用户组内唯一
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param name 节目名称
     * @param excludeId 排除的节目ID（用于更新时检查）
     * @return 是否可用
     */
    boolean isNameAvailableInUserGroup(Long oid, Long ugid, String name, Long excludeId);
    
    /**
     * 保存节目
     * @param program 节目信息
     * @return 保存后的节目
     */
    Program save(Program program);
    
    /**
     * 删除节目
     * @param programId 节目ID
     * @return 删除的记录数
     */
    int deleteById(Long programId);
    
    /**
     * 批量删除节目
     * @param programIds 节目ID列表
     * @return 删除的记录数
     */
    int deleteByIds(List<Long> programIds);
    
    /**
     * 更新节目状态
     * @param programId 节目ID
     * @param status 新状态
     * @param updatedBy 更新者ID
     * @return 更新的记录数
     */
    int updateStatus(Long programId, ProgramStatusEnum status, Long updatedBy);
    
    /**
     * 更新节目审核状态
     * @param programId 节目ID
     * @param approvalStatus 审核状态
     * @param updatedBy 更新者ID
     * @return 更新的记录数
     */
    int updateApprovalStatus(Long programId, ProgramApprovalStatusEnum approvalStatus, Long updatedBy);
    
    /**
     * 批量更新节目状态
     * @param programIds 节目ID列表
     * @param status 新状态
     * @param updatedBy 更新者ID
     * @return 更新的记录数
     */
    int updateStatusBatch(List<Long> programIds, ProgramStatusEnum status, Long updatedBy);
    
    /**
     * 增加使用次数
     * @param programId 节目ID
     * @return 更新的记录数
     */
    int incrementUsageCount(Long programId);
    
    /**
     * 查询热门节目（按使用次数排序）
     * @param oid 组织ID
     * @param ugid 用户组ID（可选）
     * @param limit 限制数量
     * @return 热门节目列表
     */
    List<Program> findPopularPrograms(Long oid, Long ugid, int limit);
    
    /**
     * 检查节目是否存在
     * @param programId 节目ID
     * @return 是否存在
     */
    boolean existsById(Long programId);
    
    /**
     * 检查用户是否有节目访问权限
     * @param programId 节目ID
     * @param userId 用户ID
     * @param oid 用户所属组织ID
     * @return 是否有权限
     */
    boolean hasAccessPermission(Long programId, Long userId, Long oid);
    
    // ===== 版本链管理方法 =====
    
    /**
     * 根据原始节目ID查询所有版本
     * @param sourceProgramId 原始节目ID
     * @return 版本列表（按版本号排序）
     */
    List<Program> findVersionsBySourceProgramId(Long sourceProgramId);
    
    /**
     * 获取节目的最新版本
     * @param sourceProgramId 原始节目ID（可以是任意版本ID，会自动查找原始节目）
     * @return 最新版本节目
     */
    Optional<Program> findLatestVersionBySourceProgramId(Long sourceProgramId);
    
    /**
     * 根据任意版本ID查找原始节目ID
     * @param programId 任意版本的节目ID
     * @return 原始节目ID
     */
    Optional<Long> findSourceProgramIdByAnyVersion(Long programId);
    
    /**
     * 根据原始节目ID和版本号查询特定版本
     * @param sourceProgramId 原始节目ID
     * @param version 版本号
     * @return 特定版本节目
     */
    Optional<Program> findBySourceProgramIdAndVersion(Long sourceProgramId, Integer version);
    
    /**
     * 获取节目的下一个版本号
     * @param sourceProgramId 原始节目ID
     * @return 下一个版本号
     */
    Integer getNextVersionNumber(Long sourceProgramId);
    
    /**
     * 检查指定版本是否存在
     * @param sourceProgramId 原始节目ID
     * @param version 版本号
     * @return 是否存在
     */
    boolean existsBySourceProgramIdAndVersion(Long sourceProgramId, Integer version);
    
    /**
     * 获取节目的版本数量
     * @param sourceProgramId 原始节目ID
     * @return 版本数量
     */
    long countVersionsBySourceProgramId(Long sourceProgramId);

    /**
     * 更新VSN生成结果字段
     * @param programId 节目ID
     * @param status VSN生成状态
     * @param vsnFileId VSN文件ID（可空）
     * @param vsnFilePath VSN文件路径（可空）
     * @param errorMessage 错误信息（可空）
     * @param updatedBy 更新者（系统更新可传0）
     * @return 影响行数
     */
    int updateVsnGenerationResult(Long programId,
                                  org.nan.cloud.program.enums.VsnGenerationStatusEnum status,
                                  String vsnFileId,
                                  String vsnFilePath,
                                  String errorMessage,
                                  Long updatedBy);
    
    /**
     * 更新节目缩略图URL
     * @param programId 节目ID
     * @param thumbnailUrl 缩略图URL
     * @param updatedBy 更新者
     * @return 影响行数
     */
    int updateThumbnailUrl(Long programId, String thumbnailUrl, Long updatedBy);
    
    // ===== 模板管理相关方法 =====
    
    /**
     * 查询用户组模板列表（支持继承）
     * 继承规则：子用户组可以继承父用户组的模板
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param keyword 搜索关键词（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 模板分页结果（包含继承的模板）
     */
    PageVO<Program> findTemplatesWithInheritance(Long oid, Long ugid, String keyword, int page, int size);
}