package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.program.dto.request.CreateProgramRequest;
import org.nan.cloud.program.dto.request.UpdateProgramRequest;
import org.nan.cloud.program.dto.response.ProgramDTO;
import org.nan.cloud.program.dto.response.ProgramDetailDTO;
import org.nan.cloud.program.dto.response.ProgramVersionDTO;
import org.nan.cloud.program.enums.ProgramStatusEnum;

import java.util.List;
import java.util.Optional;

/**
 * 节目管理服务接口
 * 处理节目的核心业务逻辑
 */
public interface ProgramService {
    
    /**
     * 创建节目
     * @param request 节目创建请求
     * @param userId 创建者用户ID
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @return 创建的节目信息
     */
    ProgramDTO createProgram(CreateProgramRequest request, Long userId, Long oid, Long ugid);
    
    /**
     * 更新节目（创建新版本）
     * @param programId 节目ID
     * @param request 更新请求
     * @param userId 更新者用户ID
     * @param oid 组织ID
     * @return 新版本节目信息
     */
    ProgramDTO updateProgram(Long programId, UpdateProgramRequest request, Long userId, Long oid);
    
    /**
     * 根据ID查询节目基本信息
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 节目基本信息
     */
    Optional<ProgramDTO> findProgramById(Long programId, Long oid);
    
    /**
     * 查询节目完整详情（包含MongoDB内容）
     * @param programId 节目ID
     * @param oid 组织ID
     * @return 节目完整详情
     */
    Optional<ProgramDetailDTO> findProgramDetails(Long programId, Long oid);
    
    /**
     * 分页查询用户组节目列表
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 节目列表
     */
    List<ProgramDTO> findProgramsByUserGroup(Long oid, Long ugid, ProgramStatusEnum status, int page, int size);

    /**
     * 分页查询用户组节目列表（支持关键词搜索）
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param keyword 搜索关键词（可选）
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 分页结果
     */
    PageVO<ProgramDTO> findProgramsPage(Long oid, Long ugid, String keyword, ProgramStatusEnum status, int page, int size);
    
    /**
     * 统计用户组节目数量
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param status 节目状态（可选）
     * @return 节目数量
     */
    long countProgramsByUserGroup(Long oid, Long ugid, ProgramStatusEnum status);
    
    /**
     * 分页查询用户创建的节目列表
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @param status 节目状态（可选）
     * @param page 页码，从1开始
     * @param size 每页大小
     * @return 节目列表
     */
    List<ProgramDTO> findProgramsByCreator(Long oid, Long createdBy, ProgramStatusEnum status, int page, int size);
    
    /**
     * 统计用户创建的节目数量
     * @param oid 组织ID
     * @param createdBy 创建者用户ID
     * @param status 节目状态（可选）
     * @return 节目数量
     */
    long countProgramsByCreator(Long oid, Long createdBy, ProgramStatusEnum status);
    
    /**
     * 删除节目
     * @param programId 节目ID
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 是否删除成功
     */
    boolean deleteProgram(Long programId, Long userId, Long oid);
    
    /**
     * 批量删除节目
     * @param programIds 节目ID列表
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 删除的节目数量
     */
    int deleteProgramsBatch(List<Long> programIds, Long userId, Long oid);
    
    /**
     * 更新节目状态
     * @param programId 节目ID
     * @param status 新状态
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 是否更新成功
     */
    boolean updateProgramStatus(Long programId, ProgramStatusEnum status, Long userId, Long oid);
    
    /**
     * 检查节目名称在用户组内是否可用
     * @param oid 组织ID
     * @param ugid 用户组ID
     * @param name 节目名称
     * @param excludeId 排除的节目ID（用于更新时检查）
     * @return 是否可用
     */
    boolean isProgramNameAvailable(Long oid, Long ugid, String name, Long excludeId);
    
    /**
     * 增加节目使用次数
     * @param programId 节目ID
     * @return 是否操作成功
     */
    boolean incrementUsageCount(Long programId);
    
    /**
     * 查询热门节目
     * @param oid 组织ID
     * @param ugid 用户组ID（可选）
     * @param limit 限制数量
     * @return 热门节目列表
     */
    List<ProgramDTO> findPopularPrograms(Long oid, Long ugid, int limit);
    
    /**
     * 检查用户是否有节目访问权限
     * @param programId 节目ID
     * @param userId 用户ID
     * @param oid 用户所属组织ID
     * @return 是否有权限
     */
    boolean hasAccessPermission(Long programId, Long userId, Long oid);
    
    // ===== 版本控制相关方法 =====
    
    /**
     * 查询节目的所有版本
     * @param sourceProgramId 原始节目ID（可以是任意版本ID）
     * @param oid 组织ID
     * @return 版本列表（按版本号排序）
     */
    List<ProgramVersionDTO> findAllVersions(Long sourceProgramId, Long oid);
    
    /**
     * 查询节目的最新版本
     * @param sourceProgramId 原始节目ID（可以是任意版本ID）
     * @param oid 组织ID
     * @return 最新版本节目
     */
    Optional<ProgramDTO> findLatestVersion(Long sourceProgramId, Long oid);
    
    /**
     * 查询指定版本的节目
     * @param sourceProgramId 原始节目ID
     * @param version 版本号
     * @param oid 组织ID
     * @return 指定版本节目
     */
    Optional<ProgramDTO> findSpecificVersion(Long sourceProgramId, Integer version, Long oid);
    
    /**
     * 基于现有版本创建新版本
     * @param baseProgramId 基础版本ID
     * @param request 更新内容
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 新版本节目信息
     */
    ProgramDTO createNewVersion(Long baseProgramId, UpdateProgramRequest request, Long userId, Long oid);
    
    /**
     * 版本回滚（创建基于历史版本的新版本）
     * @param sourceProgramId 原始节目ID
     * @param targetVersion 目标版本号
     * @param userId 操作者用户ID
     * @param oid 组织ID
     * @return 新版本节目信息
     */
    ProgramDTO rollbackToVersion(Long sourceProgramId, Integer targetVersion, Long userId, Long oid);
    
    /**
     * 获取版本链信息
     * @param programId 任意版本的节目ID
     * @param oid 组织ID
     * @return 版本链信息
     */
    Optional<ProgramVersionDTO> getVersionChainInfo(Long programId, Long oid);
}