package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.Program;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.ProgramDomainConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.ProgramMapper;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.program.entity.ProgramDO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.core.event.quota.QuotaChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节目Repository实现
 * 符合DDD Infrastructure层职责：处理数据持久化和Domain对象转换
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProgramRepositoryImpl implements ProgramRepository {

    private final ProgramMapper programMapper;
    private final ProgramDomainConverter programDomainConverter;
    private final UserGroupRepository userGroupRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Optional<Program> findById(Long programId) {
        log.debug("Finding program by id: {}", programId);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", programId).eq("deleted", 0);
        
        ProgramDO programDO = programMapper.selectOne(queryWrapper);
        return Optional.ofNullable(programDO)
                .map(programDomainConverter::toDomain);
    }

    @Override
    public List<Program> findByIds(List<Long> programIds) {
        if (CollectionUtils.isEmpty(programIds)) {
            return List.of();
        }
        
        log.debug("Batch finding programs by ids: {}, count: {}", programIds, programIds.size());
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", programIds).eq("deleted", 0);
        
        List<ProgramDO> programDOs = programMapper.selectList(queryWrapper);
        List<Program> programs = programDomainConverter.toDomains(programDOs);
        
        log.debug("Found {} programs for {} ids", programs.size(), programIds.size());
        return programs;
    }

    @Override
    public Optional<Program> findByIdAndOid(Long programId, Long oid) {
        log.debug("Finding program by id: {} and oid: {}", programId, oid);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", programId).eq("org_id", oid).eq("deleted", 0);
        
        ProgramDO programDO = programMapper.selectOne(queryWrapper);
        return Optional.ofNullable(programDO)
                .map(programDomainConverter::toDomain);
    }

    @Override
    public List<Program> findByUserGroup(Long oid, Long ugid, ProgramStatusEnum status, int page, int size) {
        log.debug("Finding programs by user group: oid={}, ugid={}, status={}, page={}, size={}", 
                oid, ugid, status, page, size);
        
        Page<ProgramDO> pageParam = new Page<>(page, size);
        IPage<ProgramDO> result = programMapper.selectPageByUserGroup(pageParam, oid, ugid, status);
        
        return programDomainConverter.toDomains(result.getRecords());
    }

    @Override
    public PageVO<Program> findProgramsPage(Long oid, Long ugid, String keyword, ProgramStatusEnum status, int page, int size) {
        log.debug("Finding programs page: oid={}, ugid={}, keyword={}, status={}, page={}, size={}", 
                oid, ugid, keyword, status, page, size);
        
        Page<ProgramDO> pageParam = new Page<>(page, size);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("org_id", oid)
                   .eq("user_group_id", ugid)  
                   .eq("deleted", 0);
        
        // 关键词搜索（节目名称或描述）
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like("name", keyword.trim())
                                              .or()
                                              .like("description", keyword.trim()));
        }
        
        // 状态过滤
        if (status != null) {
            queryWrapper.eq("program_status", status);
        }
        
        // 按更新时间倒序
        queryWrapper.orderByDesc("updated_time");
        
        IPage<ProgramDO> result = programMapper.selectPage(pageParam, queryWrapper);
        
        // 转换为Domain对象的IPage
        List<Program> domainList = programDomainConverter.toDomains(result.getRecords());

        PageVO<Program> pageVO = PageVO.<Program>builder()
                .records(domainList)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .build();
        pageVO.calculate();
        return pageVO;
    }

    @Override
    public long countByUserGroup(Long oid, Long ugid, ProgramStatusEnum status) {
        log.debug("Counting programs by user group: oid={}, ugid={}, status={}", oid, ugid, status);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("org_id", oid)
                   .eq("user_group_id", ugid)
                   .eq("deleted", 0);
        
        if (status != null) {
            queryWrapper.eq("program_status", status);
        }
        
        return programMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Program> findByCreator(Long oid, Long createdBy, ProgramStatusEnum status, int page, int size) {
        log.debug("Finding programs by creator: oid={}, createdBy={}, status={}, page={}, size={}", 
                oid, createdBy, status, page, size);
        
        Page<ProgramDO> pageParam = new Page<>(page, size);
        IPage<ProgramDO> result = programMapper.selectPageByCreator(pageParam, oid, createdBy, status);
        
        return programDomainConverter.toDomains(result.getRecords());
    }

    @Override
    public long countByCreator(Long oid, Long createdBy, ProgramStatusEnum status) {
        log.debug("Counting programs by creator: oid={}, createdBy={}, status={}", oid, createdBy, status);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("org_id", oid)
                   .eq("created_by", createdBy)
                   .eq("deleted", 0);
        
        if (status != null) {
            queryWrapper.eq("program_status", status);
        }
        
        return programMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Program> findByOrganization(Long oid, ProgramStatusEnum status, int page, int size) {
        log.debug("Finding programs by organization: oid={}, status={}, page={}, size={}", 
                oid, status, page, size);
        
        Page<ProgramDO> pageParam = new Page<>(page, size);
        IPage<ProgramDO> result = programMapper.selectPageByOrganization(pageParam, oid, status);
        
        return programDomainConverter.toDomains(result.getRecords());
    }

    @Override
    public long countByOrganization(Long oid, ProgramStatusEnum status) {
        log.debug("Counting programs by organization: oid={}, status={}", oid, status);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("org_id", oid).eq("deleted", 0);
        
        if (status != null) {
            queryWrapper.eq("program_status", status);
        }
        
        return programMapper.selectCount(queryWrapper);
    }

    @Override
    public boolean isNameAvailableInUserGroup(Long oid, Long ugid, String name, Long excludeId) {
        log.debug("Checking name availability: oid={}, ugid={}, name={}, excludeId={}", 
                oid, ugid, name, excludeId);
        
        long count = programMapper.countByNameInUserGroup(oid, ugid, name, excludeId);
        return count == 0;
    }

    @Override
    public Program save(Program program) {
        log.debug("Saving program: id={}, name={}", program.getId(), program.getName());
        
        ProgramDO programDO = programDomainConverter.toDO(program);
        
        if (programDO.getId() == null) {
            // 新增
            programDO.setCreatedTime(LocalDateTime.now());
            programDO.setUpdatedTime(LocalDateTime.now());
            programMapper.insert(programDO);
            log.debug("Program inserted with id: {}", programDO.getId());
        } else {
            // 更新
            programDO.setUpdatedTime(LocalDateTime.now());
            int updatedRows = programMapper.updateById(programDO);
            log.debug("Program updated: id={}, affected rows={}", programDO.getId(), updatedRows);
            
            if (updatedRows == 0) {
                log.warn("No rows updated for program: {}", programDO.getId());
            }
        }
        
        return programDomainConverter.toDomain(programDO);
    }

    @Override
    public int deleteById(Long programId) {
        log.debug("Deleting program by id: {}", programId);
        
        // 删除前检查VSN文件，发布配额回收事件
        try {
            Optional<Program> program = findById(programId);
            if (program.isPresent() && program.get().getVsnFileSize() != null && program.get().getVsnFileSize() > 0) {
                QuotaChangeEvent event = new QuotaChangeEvent(
                        this, 
                        QuotaChangeEvent.QuotaChangeEventType.VSN_DELETE, 
                        String.valueOf(programId)
                );
                applicationEventPublisher.publishEvent(event);
                log.debug("发布VSN删除事件: programId={}, size={}",
                        programId, program.get().getVsnFileSize());
            }
        } catch (Exception e) {
            log.error("Failed to publish VSN delete quota event: programId={}, error={}", 
                    programId, e.getMessage(), e);
            // 配额事件发布失败不影响删除操作
        }
        
        int deletedRows = programMapper.deleteById(programId);
        log.debug("Program deleted: id={}, affected rows={}", programId, deletedRows);
        
        return deletedRows;
    }

    @Override
    public int deleteByIds(List<Long> programIds) {
        log.debug("Batch deleting programs: ids={}", programIds);
        
        if (CollectionUtils.isEmpty(programIds)) {
            return 0;
        }
        
        // 批量删除前检查VSN文件，发布配额回收事件
        try {
            List<Program> programs = findByIds(programIds);
            for (Program program : programs) {
                if (program.getVsnFileSize() != null && program.getVsnFileSize() > 0) {
                    QuotaChangeEvent event = new QuotaChangeEvent(
                            this, 
                            QuotaChangeEvent.QuotaChangeEventType.VSN_DELETE, 
                            String.valueOf(program.getId())
                    );
                    applicationEventPublisher.publishEvent(event);
                    log.debug("批量发送VSN删除事件: programId={}, size={}",
                            program.getId(), program.getVsnFileSize());
                }
            }
        } catch (Exception e) {
            log.error("发布VSN删除事件失败: programIds={}, error={}",
                    programIds, e.getMessage(), e);
            // 配额事件发布失败不影响删除操作
        }
        
        // 使用MyBatis Plus的deleteBatchIds方法
        int deletedRows = programMapper.deleteBatchIds(programIds);
        log.debug("Programs batch deleted: count={}, affected rows={}", programIds.size(), deletedRows);
        
        return deletedRows;
    }

    @Override
    public int updateStatus(Long programId, ProgramStatusEnum status, Long updatedBy) {
        log.debug("Updating program status: id={}, status={}, updatedBy={}", programId, status, updatedBy);
        
        UpdateWrapper<ProgramDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", programId)
                    .set("program_status", status)
                    .set("updated_by", updatedBy)
                    .set("updated_time", LocalDateTime.now());
        
        int updatedRows = programMapper.update(null, updateWrapper);
        log.debug("Program status updated: id={}, affected rows={}", programId, updatedRows);
        
        return updatedRows;
    }
    
    @Override
    public int updateApprovalStatus(Long programId, ProgramApprovalStatusEnum approvalStatus, Long updatedBy) {
        log.debug("Updating program approval status: id={}, approvalStatus={}, updatedBy={}", 
                programId, approvalStatus, updatedBy);
        
        UpdateWrapper<ProgramDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", programId)
                    .set("program_approval_status", approvalStatus)
                    .set("updated_by", updatedBy)
                    .set("updated_time", LocalDateTime.now());
        
        int updatedRows = programMapper.update(null, updateWrapper);
        log.debug("Program approval status updated: id={}, affected rows={}", programId, updatedRows);
        
        return updatedRows;
    }

    @Override
    public int updateStatusBatch(List<Long> programIds, ProgramStatusEnum status, Long updatedBy) {
        log.debug("Batch updating program status: ids={}, status={}, updatedBy={}", 
                programIds, status, updatedBy);
        
        if (CollectionUtils.isEmpty(programIds)) {
            return 0;
        }
        
        UpdateWrapper<ProgramDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", programIds)
                    .set("program_status", status)
                    .set("updated_by", updatedBy)
                    .set("updated_time", LocalDateTime.now());
        
        int updatedRows = programMapper.update(null, updateWrapper);
        log.debug("Programs status batch updated: count={}, affected rows={}", programIds.size(), updatedRows);
        
        return updatedRows;
    }

    @Override
    public int incrementUsageCount(Long programId) {
        log.debug("Incrementing usage count for program: {}", programId);
        
        int updatedRows = programMapper.incrementUsageCount(programId);
        log.debug("Usage count incremented: id={}, affected rows={}", programId, updatedRows);
        
        return updatedRows;
    }

    @Override
    public List<Program> findPopularPrograms(Long oid, Long ugid, int limit) {
        log.debug("Finding popular programs: oid={}, ugid={}, limit={}", oid, ugid, limit);
        
        List<ProgramDO> programDOs = programMapper.selectPopularPrograms(oid, ugid, limit);
        return programDomainConverter.toDomains(programDOs);
    }

    @Override
    public boolean existsById(Long programId) {
        log.debug("Checking if program exists: id={}", programId);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", programId).eq("deleted", 0);
        
        Long count = programMapper.selectCount(queryWrapper);
        return count > 0;
    }

    @Override
    public boolean hasAccessPermission(Long programId, Long userId, Long oid) {
        log.debug("Checking access permission: programId={}, userId={}, oid={}", programId, userId, oid);
        
        long count = programMapper.countAccessibleByUser(programId, userId, oid);
        return count > 0;
    }

    // ===== 版本控制相关方法实现 =====

    @Override
    public List<Program> findVersionsBySourceProgramId(Long sourceProgramId) {
        log.debug("Finding versions by source program id: {}", sourceProgramId);
        
        List<ProgramDO> programDOs = programMapper.selectVersionsBySourceProgramId(sourceProgramId);
        return programDomainConverter.toDomains(programDOs);
    }

    @Override
    public Optional<Program> findLatestVersionBySourceProgramId(Long sourceProgramId) {
        log.debug("Finding latest version by source program id: {}", sourceProgramId);
        
        ProgramDO programDO = programMapper.selectLatestVersionBySourceProgramId(sourceProgramId);
        return Optional.ofNullable(programDO)
                .map(programDomainConverter::toDomain);
    }

    @Override
    public Optional<Long> findSourceProgramIdByAnyVersion(Long programId) {
        log.debug("Finding source program id by any version: {}", programId);
        
        Long sourceProgramId = programMapper.selectSourceProgramIdByAnyVersion(programId);
        return Optional.ofNullable(sourceProgramId);
    }

    @Override
    public Optional<Program> findBySourceProgramIdAndVersion(Long sourceProgramId, Integer version) {
        log.debug("Finding program by source id and version: sourceProgramId={}, version={}", 
                sourceProgramId, version);
        
        ProgramDO programDO = programMapper.selectBySourceProgramIdAndVersion(sourceProgramId, version);
        return Optional.ofNullable(programDO)
                .map(programDomainConverter::toDomain);
    }

    @Override
    public Integer getNextVersionNumber(Long sourceProgramId) {
        log.debug("Getting next version number for source program: {}", sourceProgramId);
        
        Integer maxVersion = programMapper.selectMaxVersionBySourceProgramId(sourceProgramId);
        Integer nextVersion = (maxVersion != null ? maxVersion : 0) + 1;
        
        log.debug("Next version number: {} for source program: {}", nextVersion, sourceProgramId);
        return nextVersion;
    }

    @Override
    public boolean existsBySourceProgramIdAndVersion(Long sourceProgramId, Integer version) {
        log.debug("Checking if version exists: sourceProgramId={}, version={}", sourceProgramId, version);
        
        long count = programMapper.countBySourceProgramIdAndVersion(sourceProgramId, version);
        return count > 0;
    }

    @Override
    public long countVersionsBySourceProgramId(Long sourceProgramId) {
        log.debug("Counting versions by source program id: {}", sourceProgramId);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0)
                   .and(wrapper -> wrapper
                           .eq("source_program_id", sourceProgramId)
                           .or()
                           .eq("id", sourceProgramId).eq("is_source_program", true)
                   );
        
        return programMapper.selectCount(queryWrapper);
    }

    @Override
    public int updateVsnGenerationResult(Long programId,
                                         org.nan.cloud.program.enums.VsnGenerationStatusEnum status,
                                         String vsnFileId,
                                         String vsnFilePath,
                                         String errorMessage,
                                         Long updatedBy) {
        log.debug("Updating VSN result: programId={}, status={}, fileId={}, path={}",
                programId, status, vsnFileId, vsnFilePath);

        UpdateWrapper<ProgramDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", programId)
                .set("vsn_generation_status", status)
                .set("vsn_file_id", vsnFileId)
                .set("vsn_file_path", vsnFilePath)
                .set("vsn_generation_error", errorMessage)
                .set("updated_by", updatedBy)
                .set("updated_time", LocalDateTime.now());

        return programMapper.update(null, updateWrapper);
    }

    @Override
    public int updateVsnGenerationResult(Long programId,
                                         org.nan.cloud.program.enums.VsnGenerationStatusEnum status,
                                         String vsnFileId,
                                         String vsnFilePath,
                                         Long vsnFileSize,
                                         String errorMessage,
                                         Long updatedBy) {
        log.debug("Updating VSN result with file size: programId={}, status={}, fileId={}, path={}, size={}",
                programId, status, vsnFileId, vsnFilePath, vsnFileSize);

        UpdateWrapper<ProgramDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", programId)
                .set("vsn_generation_status", status)
                .set("vsn_file_id", vsnFileId)
                .set("vsn_file_path", vsnFilePath)
                .set("vsn_file_size", vsnFileSize)
                .set("vsn_generation_error", errorMessage)
                .set("updated_by", updatedBy)
                .set("updated_time", LocalDateTime.now());

        return programMapper.update(null, updateWrapper);
    }
    
    @Override
    public int updateThumbnailUrl(Long programId, String thumbnailUrl, Long updatedBy) {
        log.debug("Updating program thumbnail: programId={}, thumbnailUrl={}", programId, thumbnailUrl);
        
        UpdateWrapper<ProgramDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", programId)
                .set("thumbnail_url", thumbnailUrl)
                .set("updated_by", updatedBy)
                .set("updated_time", LocalDateTime.now());
        
        return programMapper.update(null, updateWrapper);
    }

    // ===== 模板管理相关方法实现 =====

    /**
     * 查询用户组模板列表（支持继承）
     * 
     * 性能优化建议：
     * 1. 建议添加数据库索引：
     *    - CREATE INDEX idx_program_template_query ON t_program(org_id, program_status, user_group_id, updated_time DESC, deleted);
     *    - CREATE INDEX idx_program_template_search ON t_program(org_id, program_status, name, description, deleted);
     * 
     * 2. 查询优化策略：
     *    - 使用复合索引覆盖WHERE和ORDER BY条件
     *    - 考虑分页查询的LIMIT优化
     *    - 对于大数据集，考虑使用游标分页替代OFFSET分页
     * 
     * 3. 用户组继承查询优化：
     *    - 用户组层级不深时（<5层），当前实现性能良好
     *    - 如果用户组层级很深，考虑使用递归CTE或层级表设计
     *    - 可以考虑缓存用户组继承关系以提升查询性能
     */
    @Override
    public PageVO<Program> findTemplatesWithInheritance(Long oid, Long ugid, String keyword, int page, int size) {
        log.debug("Finding templates with inheritance: oid={}, ugid={}, keyword={}, page={}, size={}", 
                oid, ugid, keyword, page, size);
        
        // 1. 获取用户组及其所有父级用户组的ID列表（支持继承）
        List<Long> inheritedUgids;
        try {
            inheritedUgids = userGroupRepository.getAllUgidsByParent(ugid);
            log.debug("Found {} inherited user groups for ugid={}: {}", inheritedUgids.size(), ugid, inheritedUgids);
        } catch (Exception e) {
            log.warn("Failed to get inherited user groups for ugid={}, fallback to current group only", ugid, e);
            inheritedUgids = List.of(ugid);
        }
        
        if (inheritedUgids.isEmpty()) {
            inheritedUgids = List.of(ugid);
        }
        
        Page<ProgramDO> pageParam = new Page<>(page, size);
        
        QueryWrapper<ProgramDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("org_id", oid)
                   .eq("program_status", ProgramStatusEnum.TEMPLATE)
                   .eq("deleted", 0);
        
        // 2. 用户组继承查询：当前用户组 + 所有父级用户组的模板
        queryWrapper.in("user_group_id", inheritedUgids);
        
        // 3. 关键词搜索（模板名称或描述）
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like("name", keyword.trim())
                                              .or()
                                              .like("description", keyword.trim()));
        }
        
        // 4. 按更新时间倒序
        queryWrapper.orderByDesc("updated_time");
        
        IPage<ProgramDO> result = programMapper.selectPage(pageParam, queryWrapper);
        
        // 5. 转换为Domain对象
        List<Program> domainList = programDomainConverter.toDomains(result.getRecords());

        PageVO<Program> pageVO = PageVO.<Program>builder()
                .records(domainList)
                .total(result.getTotal())
                .pageNum((int) result.getCurrent())
                .pageSize((int) result.getSize())
                .build();
        pageVO.calculate();
        
        log.info("Found {} templates for oid={}, ugid={} with inheritance from {} user groups", 
                result.getTotal(), oid, ugid, inheritedUgids.size());
        return pageVO;
    }
}