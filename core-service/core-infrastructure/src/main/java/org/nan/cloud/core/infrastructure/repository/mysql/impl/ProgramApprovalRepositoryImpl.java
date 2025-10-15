package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.domain.ProgramApproval;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.ProgramApprovalConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.ProgramApprovalMapper;
import org.nan.cloud.core.repository.ProgramApprovalRepository;
import org.nan.cloud.program.entity.ProgramApprovalDO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节目审核Repository实现
 * 符合DDD Infrastructure层职责：处理数据持久化和Domain对象转换
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProgramApprovalRepositoryImpl implements ProgramApprovalRepository {

    private final ProgramApprovalMapper programApprovalMapper;
    private final ProgramApprovalConverter programApprovalConverter;

    @Override
    public ProgramApproval findByApprovalId(Long approvalId) {
        return programApprovalConverter.toDomain(programApprovalMapper.selectById(approvalId));
    }

    @Override
    public List<ProgramApproval> findByProgramId(Long programId) {
        log.debug("🔍 查询节目审核记录 - programId: {}", programId);
        
        List<ProgramApprovalDO> approvalDOs = programApprovalMapper.selectByProgramId(programId);
        
        if (CollectionUtils.isEmpty(approvalDOs)) {
            log.debug("📭 未找到节目审核记录 - programId: {}", programId);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalConverter.toDomains(approvalDOs);
        log.debug("✅ 查询到 {} 条审核记录 - programId: {}", approvals.size(), programId);
        return approvals;
    }

    @Override
    public Optional<ProgramApproval> findByProgramIdAndVersion(Long programId, Integer version) {
        log.debug("🔍 查询节目版本审核记录 - programId: {}, version: {}", programId, version);
        
        ProgramApprovalDO approvalDO = programApprovalMapper.selectByProgramIdAndVersion(programId, version);
        
        if (approvalDO == null) {
            log.debug("📭 未找到节目版本审核记录 - programId: {}, version: {}", programId, version);
            return Optional.empty();
        }
        
        ProgramApproval approval = programApprovalConverter.toDomain(approvalDO);
        log.debug("✅ 找到节目版本审核记录 - programId: {}, version: {}, status: {}", 
                programId, version, approval.getStatus());
        return Optional.of(approval);
    }

    @Override
    public Optional<ProgramApproval> findLatestByProgramId(Long programId) {
        log.debug("🔍 查询节目最新审核记录 - programId: {}", programId);
        
        ProgramApprovalDO approvalDO = programApprovalMapper.selectLatestByProgramId(programId);
        
        if (approvalDO == null) {
            log.debug("📭 未找到节目最新审核记录 - programId: {}", programId);
            return Optional.empty();
        }
        
        ProgramApproval approval = programApprovalConverter.toDomain(approvalDO);
        log.debug("✅ 找到节目最新审核记录 - programId: {}, version: {}, status: {}", 
                programId, approval.getProgramVersion(), approval.getStatus());
        return Optional.of(approval);
    }

    @Override
    public List<ProgramApproval> findPendingByOrganization(Long oid, int page, int size) {
        log.debug("🔍 分页查询组织待审核记录 - oid: {}, page: {}, size: {}", oid, page, size);
        
        Page<ProgramApprovalDO> myBatisPage = new Page<>(page, size);
        var resultPage = programApprovalMapper.selectPendingByOrganization(myBatisPage, oid);
        List<ProgramApprovalDO> approvalDOs = resultPage.getRecords();
        
        if (CollectionUtils.isEmpty(approvalDOs)) {
            log.debug("📭 未找到组织待审核记录 - oid: {}, page: {}", oid, page);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalConverter.toDomains(approvalDOs);
        log.debug("✅ 查询到 {} 条组织待审核记录 - oid: {}, page: {}", approvals.size(), oid, page);
        return approvals;
    }

    @Override
    public long countPendingByOrganization(Long oid) {
        log.debug("🔍 统计组织待审核记录数量 - oid: {}", oid);
        
        long count = programApprovalMapper.countPendingByOrganization(oid);
        log.debug("✅ 组织待审核记录数量 - oid: {}, count: {}", oid, count);
        return count;
    }

    @Override
    public List<ProgramApproval> findByReviewer(Long reviewerId, ProgramApprovalStatusEnum status, 
                                               int page, int size) {
        log.debug("🔍 分页查询审核人员审核记录 - reviewerId: {}, status: {}, page: {}, size: {}", 
                reviewerId, status, page, size);
        
        Page<ProgramApprovalDO> myBatisPage = new Page<>(page, size);
        var resultPage = programApprovalMapper.selectByReviewer(myBatisPage, reviewerId, status);
        List<ProgramApprovalDO> approvalDOs = resultPage.getRecords();
        
        if (CollectionUtils.isEmpty(approvalDOs)) {
            log.debug("📭 未找到审核人员审核记录 - reviewerId: {}, status: {}", reviewerId, status);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalConverter.toDomains(approvalDOs);
        log.debug("✅ 查询到 {} 条审核人员审核记录 - reviewerId: {}, status: {}", 
                approvals.size(), reviewerId, status);
        return approvals;
    }

    @Override
    public long countByReviewer(Long reviewerId, ProgramApprovalStatusEnum status) {
        log.debug("🔍 统计审核人员审核记录数量 - reviewerId: {}, status: {}", reviewerId, status);
        
        long count = programApprovalMapper.countByReviewer(reviewerId, status);
        log.debug("✅ 审核人员审核记录数量 - reviewerId: {}, status: {}, count: {}", 
                reviewerId, status, count);
        return count;
    }

    @Override
    public ProgramApproval save(ProgramApproval approval) {
        log.debug("💾 保存节目审核记录 - id: {}, programId: {}", approval.getId(), approval.getProgramId());
        
        ProgramApprovalDO approvalDO = programApprovalConverter.toDO(approval);
        
        if (approval.getId() == null) {
            // 新增记录
            approvalDO.setCreatedTime(LocalDateTime.now());
            approvalDO.setUpdatedTime(LocalDateTime.now());
            approvalDO.setAppliedTime(LocalDateTime.now());
            int rows = programApprovalMapper.insert(approvalDO);
            if (rows > 0) {
                approval.setId(approvalDO.getId());
                log.debug("✅ 新增节目审核记录成功 - id: {}, programId: {}", 
                        approval.getId(), approval.getProgramId());
            } else {
                log.error("❌ 新增节目审核记录失败 - programId: {}", approval.getProgramId());
            }
        } else {
            // 更新记录
            int rows = programApprovalMapper.updateById(approvalDO);
            if (rows > 0) {
                log.debug("✅ 更新节目审核记录成功 - id: {}", approval.getId());
            } else {
                log.error("❌ 更新节目审核记录失败 - id: {}", approval.getId());
            }
        }
        
        return approval;
    }

    @Override
    public int updateApprovalStatus(Long approvalId, ProgramApprovalStatusEnum status, 
                                  Long reviewerId, String comment) {
        log.debug("📝 更新审核状态 - id: {}, status: {}, reviewerId: {}",
                approvalId, status, reviewerId);
        
        String rejectionReason = (status == ProgramApprovalStatusEnum.REJECTED) ? comment : null;
        int rows = programApprovalMapper.updateApprovalStatus(approvalId, status, reviewerId, comment, rejectionReason);
        
        if (rows > 0) {
            log.debug("✅ 更新审核状态成功 - id: {}, status: {}", approvalId, status);
        } else {
            log.error("❌ 更新审核状态失败 - id: {}", approvalId);
        }
        
        return rows;
    }

    @Override
    public boolean isVersionApproved(Long programId, Integer version) {
        log.debug("🔍 检查节目版本是否已通过审核 - programId: {}, version: {}", programId, version);
        
        boolean approved = programApprovalMapper.isVersionApproved(programId, version);
        log.debug("✅ 节目版本审核状态 - programId: {}, version: {}, approved: {}", 
                programId, version, approved);
        return approved;
    }

    @Override
    public int deleteById(Long approvalId) {
        log.debug("🗑️ 删除节目审核记录 - id: {}", approvalId);
        
        int rows = programApprovalMapper.deleteById(approvalId);
        
        if (rows > 0) {
            log.debug("✅ 删除节目审核记录成功 - id: {}", approvalId);
        } else {
            log.error("❌ 删除节目审核记录失败 - id: {}", approvalId);
        }
        
        return rows;
    }

    @Override
    public int deleteByProgramId(Long programId) {
        log.debug("🗑️ 删除节目相关审核记录 - programId: {}", programId);
        
        int rows = programApprovalMapper.deleteByProgramId(programId);
        
        if (rows > 0) {
            log.debug("✅ 删除节目相关审核记录成功 - programId: {}, count: {}", programId, rows);
        } else {
            log.debug("📭 无节目相关审核记录需要删除 - programId: {}", programId);
        }
        
        return rows;
    }
    
    // ===== 新增三维度查询方法实现 =====
    
    @Override
    public List<ProgramApproval> findPendingByUserGroups(List<Long> userGroupIds, Long oid, int page, int size) {
        log.debug("🔍 查询待我审核的节目列表 - userGroupIds: {}, oid: {}, page: {}, size: {}", 
                userGroupIds, oid, page, size);
        
        if (CollectionUtils.isEmpty(userGroupIds)) {
            log.debug("📭 用户组ID列表为空，返回空列表");
            return List.of();
        }
        
        Page<ProgramApprovalDO> myBatisPage = new Page<>(page, size);
        var resultPage = programApprovalMapper.selectPendingByUserGroups(myBatisPage, userGroupIds, oid);
        List<ProgramApprovalDO> approvalDOs = resultPage.getRecords();
        
        if (CollectionUtils.isEmpty(approvalDOs)) {
            log.debug("📭 未找到待我审核记录 - userGroupIds: {}, oid: {}", userGroupIds, oid);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalConverter.toDomains(approvalDOs);
        log.debug("✅ 查询到 {} 条待我审核记录 - userGroupIds: {}, oid: {}", approvals.size(), userGroupIds, oid);
        return approvals;
    }
    
    @Override
    public long countPendingByUserGroups(List<Long> userGroupIds, Long oid) {
        log.debug("🔍 统计待我审核的节目数量 - userGroupIds: {}, oid: {}", userGroupIds, oid);
        
        if (CollectionUtils.isEmpty(userGroupIds)) {
            log.debug("📭 用户组ID列表为空，返回0");
            return 0L;
        }
        
        long count = programApprovalMapper.countPendingByUserGroups(userGroupIds, oid);
        log.debug("✅ 待我审核节目数量 - userGroupIds: {}, oid: {}, count: {}", userGroupIds, oid, count);
        return count;
    }
    
    @Override
    public List<ProgramApproval> findByCreatedBy(Long createdBy, Long oid, ProgramApprovalStatusEnum status, int page, int size) {
        log.debug("🔍 查询我发起的审核申请列表 - createdBy: {}, oid: {}, status: {}, page: {}, size: {}", 
                createdBy, oid, status, page, size);
        
        Page<ProgramApprovalDO> myBatisPage = new Page<>(page, size);
        var resultPage = programApprovalMapper.selectByCreatedBy(myBatisPage, createdBy, oid, status);
        List<ProgramApprovalDO> approvalDOs = resultPage.getRecords();
        
        if (CollectionUtils.isEmpty(approvalDOs)) {
            log.debug("📭 未找到我发起的审核申请 - createdBy: {}, oid: {}, status: {}", createdBy, oid, status);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalConverter.toDomains(approvalDOs);
        log.debug("✅ 查询到 {} 条我发起的审核申请 - createdBy: {}, oid: {}, status: {}", 
                approvals.size(), createdBy, oid, status);
        return approvals;
    }
    
    @Override
    public long countByCreatedBy(Long createdBy, Long oid, ProgramApprovalStatusEnum status) {
        log.debug("🔍 统计我发起的审核申请数量 - createdBy: {}, oid: {}, status: {}", createdBy, oid, status);
        
        long count = programApprovalMapper.countByCreatedBy(createdBy, oid, status);
        log.debug("✅ 我发起的审核申请数量 - createdBy: {}, oid: {}, status: {}, count: {}", 
                createdBy, oid, status, count);
        return count;
    }
    
    @Override
    public List<ProgramApproval> findAllByUserGroups(List<Long> userGroupIds, Long oid, ProgramApprovalStatusEnum status, int page, int size) {
        log.debug("🔍 查询全部审核记录 - userGroupIds: {}, oid: {}, status: {}, page: {}, size: {}", 
                userGroupIds, oid, status, page, size);
        
        if (CollectionUtils.isEmpty(userGroupIds)) {
            log.debug("📭 用户组ID列表为空，返回空列表");
            return List.of();
        }
        
        Page<ProgramApprovalDO> myBatisPage = new Page<>(page, size);
        var resultPage = programApprovalMapper.selectAllByUserGroups(myBatisPage, userGroupIds, oid, status);
        List<ProgramApprovalDO> approvalDOs = resultPage.getRecords();
        
        if (CollectionUtils.isEmpty(approvalDOs)) {
            log.debug("📭 未找到全部审核记录 - userGroupIds: {}, oid: {}, status: {}", userGroupIds, oid, status);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalConverter.toDomains(approvalDOs);
        log.debug("✅ 查询到 {} 条全部审核记录 - userGroupIds: {}, oid: {}, status: {}", 
                approvals.size(), userGroupIds, oid, status);
        return approvals;
    }
    
    @Override
    public long countAllByUserGroups(List<Long> userGroupIds, Long oid, ProgramApprovalStatusEnum status) {
        log.debug("🔍 统计全部审核记录数量 - userGroupIds: {}, oid: {}, status: {}", userGroupIds, oid, status);
        
        if (CollectionUtils.isEmpty(userGroupIds)) {
            log.debug("📭 用户组ID列表为空，返回0");
            return 0L;
        }
        
        long count = programApprovalMapper.countAllByUserGroups(userGroupIds, oid, status);
        log.debug("✅ 全部审核记录数量 - userGroupIds: {}, oid: {}, status: {}, count: {}", 
                userGroupIds, oid, status, count);
        return count;
    }
}