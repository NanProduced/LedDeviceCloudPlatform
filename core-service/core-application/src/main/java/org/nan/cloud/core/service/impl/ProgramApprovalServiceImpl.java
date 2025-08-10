package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.domain.ProgramApproval;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.repository.ProgramApprovalRepository;
import org.nan.cloud.core.repository.ProgramRepository;
import org.nan.cloud.core.service.ProgramApprovalService;
import org.nan.cloud.core.service.UserService;
import org.nan.cloud.core.service.converter.ProgramApprovalDtoConverter;
import org.nan.cloud.program.dto.request.ApprovalRequest;
import org.nan.cloud.program.dto.response.ProgramApprovalDTO;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节目审核服务实现
 * 实现节目审核流程的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramApprovalServiceImpl implements ProgramApprovalService {

    private final ProgramApprovalRepository programApprovalRepository;
    private final ProgramRepository programRepository;
    private final ProgramApprovalDtoConverter programApprovalDtoConverter;
    private final UserService userService;

    @Override
    @Transactional
    public ProgramApprovalDTO submitApproval(Long programId, Integer programVersion, Long userId, Long oid) {
        log.debug("📝 提交节目审核申请 - programId: {}, version: {}, userId: {}, oid: {}", 
                programId, programVersion, userId, oid);
        
        // 检查是否已经有相同版本的审核记录
        Optional<ProgramApproval> existingApproval = programApprovalRepository
                .findByProgramIdAndVersion(programId, programVersion);
        
        if (existingApproval.isPresent()) {
            ProgramApproval existing = existingApproval.get();
            if (existing.getStatus() == ProgramApprovalStatusEnum.PENDING) {
                log.warn("⚠️ 该版本已有待审核记录 - programId: {}, version: {}", programId, programVersion);
                return programApprovalDtoConverter.toProgramApprovalDTO(existing);
            }
        }
        
        // 创建新的审核申请
        ProgramApproval approval = new ProgramApproval();
        approval.setProgramId(programId);
        approval.setProgramVersion(programVersion);
        approval.setStatus(ProgramApprovalStatusEnum.PENDING);
        approval.setOid(oid);
        approval.setCreatedBy(userId);
        approval.setAppliedTime(LocalDateTime.now());
        
        ProgramApproval savedApproval = programApprovalRepository.save(approval);
        
        log.debug("✅ 节目审核申请提交成功 - id: {}, programId: {}, version: {}", 
                savedApproval.getId(), programId, programVersion);
        
        return programApprovalDtoConverter.toProgramApprovalDTO(savedApproval);
    }

    @Override
    @Transactional
    public boolean approveProgram(Long approvalId, ApprovalRequest request, 
                                 Long reviewerId, Long oid) {
        log.debug("✅ 审核通过节目 - approvalId: {}, reviewerId: {}",
                approvalId, reviewerId);
        
        // 验证审核记录权限
        ExceptionEnum.PERMISSION_DENIED.throwIf(!validateApprovalAccess(approvalId, oid));
        
        // 先获取审核记录，用于更新节目状态
        ProgramApproval approval = programApprovalRepository.findByApprovalId(approvalId);
        
        // 1. 更新审核记录状态
        int approvalRows = programApprovalRepository.updateApprovalStatus(
                approvalId, 
                ProgramApprovalStatusEnum.APPROVED, 
                reviewerId, 
                request.getComment()
        );
        
        if (approvalRows > 0) {
            // 2. 同步更新节目状态（审核通过后，设置为待发布状态）
            try {
                int programRows = programRepository.updateApprovalStatus(
                        approval.getProgramId(), 
                        ProgramApprovalStatusEnum.APPROVED, 
                        reviewerId
                );
                
                // 3. 更新节目的program_status为PENDING（待发布）
                int statusRows = programRepository.updateStatus(
                        approval.getProgramId(), 
                        ProgramStatusEnum.PENDING, 
                        reviewerId
                );
                
                if (programRows > 0 && statusRows > 0) {
                    log.debug("✅ 节目审核通过，状态更新成功 - programId: {}, approvalId: {}", 
                            approval.getProgramId(), approvalId);
                    return true;
                } else {
                    log.error("❌ 节目状态更新失败 - programId: {}, approvalId: {}", 
                            approval.getProgramId(), approvalId);
                    throw new RuntimeException("节目状态更新失败");
                }
            } catch (Exception e) {
                log.error("❌ 审核通过时节目状态更新异常 - programId: {}, error: {}", 
                        approval.getProgramId(), e.getMessage(), e);
                throw e; // 抛出异常，触发事务回滚
            }
        } else {
            log.error("❌ 节目审核记录更新失败 - approvalId: {}", approvalId);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean rejectProgram(Long approvalId, ApprovalRequest request, 
                                Long reviewerId, Long oid) {
        log.debug("❌ 审核拒绝节目 - approvalId: {}, reviewerId: {}",
                approvalId, reviewerId);

        // 验证审核记录权限
        ExceptionEnum.PERMISSION_DENIED.throwIf(!validateApprovalAccess(approvalId, oid));
        
        // 先获取审核记录，用于更新节目状态
        ProgramApproval approval = programApprovalRepository.findByApprovalId(approvalId);
        String comment = StringUtils.hasText(request.getRejectionReason()) ? 
                request.getRejectionReason() : request.getComment();
        
        // 1. 更新审核记录状态
        int approvalRows = programApprovalRepository.updateApprovalStatus(
                approvalId, 
                ProgramApprovalStatusEnum.REJECTED, 
                reviewerId,
                comment
        );
        
        if (approvalRows > 0) {
            // 2. 同步更新节目的审核状态为REJECTED（但不更新program_status，保持为null）
            try {
                int programRows = programRepository.updateApprovalStatus(
                        approval.getProgramId(), 
                        ProgramApprovalStatusEnum.REJECTED, 
                        reviewerId
                );
                
                if (programRows > 0) {
                    log.debug("✅ 节目审核拒绝，状态更新成功 - programId: {}, approvalId: {}", 
                            approval.getProgramId(), approvalId);
                    return true;
                } else {
                    log.error("❌ 节目审核状态更新失败 - programId: {}, approvalId: {}", 
                            approval.getProgramId(), approvalId);
                    throw new RuntimeException("节目审核状态更新失败");
                }
            } catch (Exception e) {
                log.error("❌ 审核拒绝时节目状态更新异常 - programId: {}, error: {}", 
                        approval.getProgramId(), e.getMessage(), e);
                throw e; // 抛出异常，触发事务回滚
            }
        } else {
            log.error("❌ 节目审核记录更新失败 - approvalId: {}", approvalId);
            return false;
        }
    }

    @Override
    public List<ProgramApprovalDTO> getProgramApprovalHistory(Long programId, Long oid) {
        log.debug("🔍 查询节目审核历史 - programId: {}, oid: {}", programId, oid);
        
        // 验证节目访问权限
        if (!validateProgramAccess(programId, oid)) {
            log.error("❌ 无权限访问节目 - programId: {}, oid: {}", programId, oid);
            return List.of();
        }
        
        List<ProgramApproval> approvals = programApprovalRepository.findByProgramId(programId);
        
        if (CollectionUtils.isEmpty(approvals)) {
            log.debug("📭 未找到节目审核历史 - programId: {}", programId);
            return List.of();
        }
        
        List<ProgramApprovalDTO> approvalDTOs = programApprovalDtoConverter.toProgramApprovalDTOs(approvals);
        // 填充审核人姓名
        fillReviewerNames(approvalDTOs);
        log.debug("✅ 查询到 {} 条节目审核历史 - programId: {}", approvalDTOs.size(), programId);
        return approvalDTOs;
    }

    @Override
    public Optional<ProgramApprovalDTO> getProgramVersionApproval(Long programId, Integer programVersion, Long oid) {
        log.debug("🔍 查询节目版本审核记录 - programId: {}, version: {}, oid: {}", 
                programId, programVersion, oid);
        
        // 验证节目访问权限
        if (!validateProgramAccess(programId, oid)) {
            log.error("❌ 无权限访问节目 - programId: {}, oid: {}", programId, oid);
            return Optional.empty();
        }
        
        Optional<ProgramApproval> approval = programApprovalRepository
                .findByProgramIdAndVersion(programId, programVersion);
        
        if (approval.isPresent()) {
            ProgramApprovalDTO approvalDTO = programApprovalDtoConverter.toProgramApprovalDTO(approval.get());
            // 填充审核人姓名
            fillReviewerName(approvalDTO);
            log.debug("✅ 找到节目版本审核记录 - programId: {}, version: {}, status: {}", 
                    programId, programVersion, approvalDTO.getStatus());
            return Optional.of(approvalDTO);
        } else {
            log.debug("📭 未找到节目版本审核记录 - programId: {}, version: {}", programId, programVersion);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProgramApprovalDTO> getLatestProgramApproval(Long programId, Long oid) {
        log.debug("🔍 查询节目最新审核记录 - programId: {}, oid: {}", programId, oid);
        
        // 验证节目访问权限
        if (!validateProgramAccess(programId, oid)) {
            log.error("❌ 无权限访问节目 - programId: {}, oid: {}", programId, oid);
            return Optional.empty();
        }
        
        Optional<ProgramApproval> approval = programApprovalRepository.findLatestByProgramId(programId);
        
        if (approval.isPresent()) {
            ProgramApprovalDTO approvalDTO = programApprovalDtoConverter.toProgramApprovalDTO(approval.get());
            // 填充审核人姓名
            fillReviewerName(approvalDTO);
            log.debug("✅ 找到节目最新审核记录 - programId: {}, version: {}, status: {}", 
                    programId, approvalDTO.getProgramVersion(), approvalDTO.getStatus());
            return Optional.of(approvalDTO);
        } else {
            log.debug("📭 未找到节目最新审核记录 - programId: {}", programId);
            return Optional.empty();
        }
    }

    @Override
    public PageVO<ProgramApprovalDTO> getPendingApprovals(Long oid, int page, int size) {
        log.debug("🔍 分页查询组织待审核列表 - oid: {}, page: {}, size: {}", oid, page, size);
        
        List<ProgramApproval> approvals = programApprovalRepository.findPendingByOrganization(oid, page, size);
        long total = programApprovalRepository.countPendingByOrganization(oid);
        
        List<ProgramApprovalDTO> approvalDTOs = programApprovalDtoConverter.toProgramApprovalDTOs(approvals);
        // 填充审核人姓名
        fillReviewerNames(approvalDTOs);
        
        PageVO<ProgramApprovalDTO> pageVO = PageVO.<ProgramApprovalDTO>builder()
                .records(approvalDTOs)
                .total(total)
                .pageNum(page)
                .pageSize(size)
                .build();
        pageVO.calculate();
        log.debug("✅ 查询到 {} 条组织待审核记录 - oid: {}, total: {}", approvalDTOs.size(), oid, total);
        return pageVO;
    }

    @Override
    public PageVO<ProgramApprovalDTO> getReviewerApprovals(Long reviewerId, ProgramApprovalStatusEnum status, 
                                                          Long oid, int page, int size) {
        log.debug("🔍 分页查询审核人员审核记录 - reviewerId: {}, status: {}, oid: {}, page: {}, size: {}", 
                reviewerId, status, oid, page, size);
        
        List<ProgramApproval> approvals = programApprovalRepository.findByReviewer(reviewerId, status, page, size);
        long total = programApprovalRepository.countByReviewer(reviewerId, status);
        
        // 过滤只返回指定组织的记录
        List<ProgramApproval> filteredApprovals = approvals.stream()
                .filter(approval -> approval.getOid().equals(oid))
                .toList();
        
        List<ProgramApprovalDTO> approvalDTOs = programApprovalDtoConverter.toProgramApprovalDTOs(filteredApprovals);
        // 填充审核人姓名
        fillReviewerNames(approvalDTOs);

        PageVO<ProgramApprovalDTO> pageVO = PageVO.<ProgramApprovalDTO>builder()
                .records(approvalDTOs)
                .total(total)
                .pageNum(page)
                .pageSize(size)
                .build();
        pageVO.calculate();
        
        log.debug("✅ 查询到 {} 条审核人员审核记录 - reviewerId: {}, status: {}", 
                approvalDTOs.size(), reviewerId, status);
        return pageVO;
    }

    @Override
    public boolean isVersionApproved(Long programId, Integer programVersion, Long oid) {
        log.debug("🔍 检查节目版本是否已通过审核 - programId: {}, version: {}, oid: {}", 
                programId, programVersion, oid);
        
        // 验证节目访问权限
        if (!validateProgramAccess(programId, oid)) {
            log.error("❌ 无权限访问节目 - programId: {}, oid: {}", programId, oid);
            return false;
        }
        
        boolean approved = programApprovalRepository.isVersionApproved(programId, programVersion);
        log.debug("✅ 节目版本审核状态 - programId: {}, version: {}, approved: {}", 
                programId, programVersion, approved);
        return approved;
    }

    @Override
    public boolean isApprovalRequired(Long oid) {
        log.debug("🔍 检查组织是否需要审核 - oid: {}", oid);
        
        // TODO: 从组织配置中获取审核设置
        // 当前硬编码为需要审核，后续可以从配置表中读取
        boolean required = true;
        
        log.debug("✅ 组织审核设置 - oid: {}, required: {}", oid, required);
        return required;
    }

    @Override
    @Transactional
    public boolean withdrawApproval(Long approvalId, Long userId, Long oid) {
        log.debug("🔄 撤销审核申请 - approvalId: {}, userId: {}, oid: {}", approvalId, userId, oid);
        
        // 验证审核记录权限和申请人身份
        if (!validateApprovalWithdraw(approvalId, userId, oid)) {
            log.error("❌ 无权限撤销审核申请 - approvalId: {}, userId: {}, oid: {}", approvalId, userId, oid);
            return false;
        }
        
        int rows = programApprovalRepository.deleteById(approvalId);
        
        if (rows > 0) {
            log.debug("✅ 撤销审核申请成功 - approvalId: {}", approvalId);
            return true;
        } else {
            log.error("❌ 撤销审核申请失败 - approvalId: {}", approvalId);
            return false;
        }
    }

    @Override
    public long getPendingApprovalCount(Long oid) {
        log.debug("🔍 统计组织待审核数量 - oid: {}", oid);
        
        long count = programApprovalRepository.countPendingByOrganization(oid);
        log.debug("✅ 组织待审核数量 - oid: {}, count: {}", oid, count);
        return count;
    }

    @Override
    public long getReviewerApprovalCount(Long reviewerId, ProgramApprovalStatusEnum status, Long oid) {
        log.debug("🔍 统计审核人员审核数量 - reviewerId: {}, status: {}, oid: {}", reviewerId, status, oid);
        
        // TODO: 这里应该按组织过滤，当前Mapper接口不支持，需要后续优化
        long count = programApprovalRepository.countByReviewer(reviewerId, status);
        log.debug("✅ 审核人员审核数量 - reviewerId: {}, status: {}, count: {}", reviewerId, status, count);
        return count;
    }
    
    /**
     * 验证审核记录访问权限
     */
    private boolean validateApprovalAccess(Long approvalId, Long oid) {
        // TODO: 实现审核记录权限验证逻辑
        // 检查审核记录是否属于指定组织
        log.debug("🔒 验证审核记录访问权限 - approvalId: {}, oid: {}", approvalId, oid);
        return true;
    }
    
    /**
     * 验证节目访问权限
     */
    private boolean validateProgramAccess(Long programId, Long oid) {
        // TODO: 实现节目权限验证逻辑
        // 检查节目是否属于指定组织
        log.debug("🔒 验证节目访问权限 - programId: {}, oid: {}", programId, oid);
        return true;
    }
    
    /**
     * 验证审核申请撤销权限
     */
    private boolean validateApprovalWithdraw(Long approvalId, Long userId, Long oid) {
        // TODO: 实现撤销权限验证逻辑
        // 检查是否为申请人本人且审核状态为待审核
        log.debug("🔒 验证审核撤销权限 - approvalId: {}, userId: {}, oid: {}", approvalId, userId, oid);
        return true;
    }
    
    /**
     * 为单个审核记录填充审核人姓名
     * 根据reviewerId查询用户信息并填充reviewerName
     */
    private void fillReviewerName(ProgramApprovalDTO approvalDTO) {
        if (approvalDTO != null && approvalDTO.getReviewerId() != null) {
            try {
                User reviewer = userService.getUserById(approvalDTO.getReviewerId());
                if (reviewer != null) {
                    approvalDTO.setReviewerName(reviewer.getUsername());
                    log.debug("📝 填充审核人姓名 - reviewerId: {}, reviewerName: {}", 
                            approvalDTO.getReviewerId(), reviewer.getUsername());
                } else {
                    log.warn("⚠️ 未找到审核人信息 - reviewerId: {}", approvalDTO.getReviewerId());
                    approvalDTO.setReviewerName("未知审核人");
                }
            } catch (Exception e) {
                log.error("❌ 填充审核人姓名失败 - reviewerId: {}, error: {}", 
                        approvalDTO.getReviewerId(), e.getMessage());
                approvalDTO.setReviewerName("获取失败");
            }
        }
    }
    
    /**
     * 为审核记录列表批量填充审核人姓名
     */
    private void fillReviewerNames(List<ProgramApprovalDTO> approvalDTOs) {
        if (approvalDTOs != null && !approvalDTOs.isEmpty()) {
            for (ProgramApprovalDTO approvalDTO : approvalDTOs) {
                fillReviewerName(approvalDTO);
            }
            log.debug("✅ 批量填充审核人姓名完成 - 共处理 {} 条记录", approvalDTOs.size());
        }
    }
}