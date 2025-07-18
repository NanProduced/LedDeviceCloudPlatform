package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.DTO.*;
import org.nan.cloud.core.DTO.BindingConflictCheckResult.ConflictType;
import org.nan.cloud.core.DTO.BindingConflictCheckResult.ConflictResolution;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.domain.UserGroupTerminalGroupBinding;
import org.nan.cloud.core.repository.UserGroupTerminalGroupBindingRepository;
import org.nan.cloud.core.service.TerminalGroupService;
import org.nan.cloud.core.service.UserGroupTerminalGroupBindingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGroupTerminalGroupBindingServiceImpl implements UserGroupTerminalGroupBindingService {

    private final UserGroupTerminalGroupBindingRepository bindingRepository;
    private final TerminalGroupService terminalGroupService;


    @Override
    public List<Long> getAccessibleTerminalGroupIds(Long ugid) {
        // 获取用户组的所有绑定关系
        List<UserGroupTerminalGroupBinding> allBindings = bindingRepository.getUserGroupBindings(ugid);
        
        // 计算包含的终端组ID集合
        Set<Long> includedTerminalGroupIds = new HashSet<>();
        Set<Long> excludedTerminalGroupIds = new HashSet<>();
        
        // 处理包含绑定
        for (UserGroupTerminalGroupBinding binding : allBindings) {
            if (binding.getBindingType() == BindingType.INCLUDE) {
                includedTerminalGroupIds.add(binding.getTgid());
                
                // 如果包含子组，添加所有子组
                if (binding.getIncludeSub()) {
                    List<TerminalGroup> childGroups = terminalGroupService.getChildGroups(binding.getTgid());
                    for (TerminalGroup childGroup : childGroups) {
                        includedTerminalGroupIds.add(childGroup.getTgid());
                    }
                }
            }
        }
        
        // 处理排除绑定
        for (UserGroupTerminalGroupBinding binding : allBindings) {
            if (binding.getBindingType() == BindingType.EXCLUDE) {
                excludedTerminalGroupIds.add(binding.getTgid());
                
                // 如果排除包含子组，排除所有子组
                if (binding.getIncludeSub()) {
                    List<TerminalGroup> childGroups = terminalGroupService.getChildGroups(binding.getTgid());
                    for (TerminalGroup childGroup : childGroups) {
                        excludedTerminalGroupIds.add(childGroup.getTgid());
                    }
                }
            }
        }
        
        // 计算最终权限：包含权限 - 排除权限
        includedTerminalGroupIds.removeAll(excludedTerminalGroupIds);
        
        return new ArrayList<>(includedTerminalGroupIds);
    }

    
    /**
     * 判断一个终端组是否是另一个终端组的子组
     */
    private boolean isChildOf(TerminalGroup child, TerminalGroup parent) {
        if (child == null || parent == null || child.getPath() == null) {
            return false;
        }
        
        // 通过路径判断父子关系
        String childPath = child.getPath();
        String parentPath = parent.getPath();
        
        // 子组的路径应该包含父组的路径
        return childPath.startsWith(parentPath + "|") || childPath.contains("|" + parent.getTgid() + "|");
    }
    
    @Override
    @Transactional
    public BatchBindingOperationResultDTO executeBatchBindingOperation(BatchBindingOperationDTO request) {
        List<BatchBindingOperationResultDTO.BindingOperationDetailDTO> operationDetails = new ArrayList<>();
        int createdBindings = 0;
        int deletedBindings = 0;
        
        // 获取用户组当前的所有绑定关系
        List<UserGroupTerminalGroupBinding> currentBindings = bindingRepository.getUserGroupBindings(request.getUgid());
        
        // 处理要绑定的终端组
        if (!CollectionUtils.isEmpty(request.getBindTgids())) {
            for (Long tgid : request.getBindTgids()) {
                BatchBindingOperationResultDTO.BindingOperationDetailDTO detail = 
                    processBindOperation(request.getUgid(), tgid, currentBindings, request.getOperatorId());
                operationDetails.add(detail);
                
                if ("CREATED_INCLUDE".equals(detail.getOperationType())) {
                    createdBindings++;
                }
            }
        }
        
        // 处理要解绑的终端组
        if (!CollectionUtils.isEmpty(request.getUnbindTgids())) {
            for (Long tgid : request.getUnbindTgids()) {
                BatchBindingOperationResultDTO.BindingOperationDetailDTO detail = 
                    processUnbindOperation(request.getUgid(), tgid, currentBindings, request.getOperatorId());
                operationDetails.add(detail);
                
                if ("DELETED_INCLUDE".equals(detail.getOperationType())) {
                    deletedBindings++;
                } else if ("CREATED_EXCLUDE".equals(detail.getOperationType())) {
                    createdBindings++;
                }
            }
        }
        
        return BatchBindingOperationResultDTO.builder()
                .success(true)
                .message("批量绑定操作完成")
                .createdBindings(createdBindings)
                .deletedBindings(deletedBindings)
                .operationDetails(operationDetails)
                .build();
    }
    
    /**
     * 处理绑定操作
     */
    private BatchBindingOperationResultDTO.BindingOperationDetailDTO processBindOperation(
            Long ugid, Long tgid, List<UserGroupTerminalGroupBinding> currentBindings, Long operatorId) {
        
        TerminalGroup targetGroup = terminalGroupService.getTerminalGroupById(tgid);
        String terminalGroupName = targetGroup != null ? targetGroup.getName() : "未知终端组";
        
        // 检查是否已经存在排除绑定
        UserGroupTerminalGroupBinding existingExcludeBinding = currentBindings.stream()
                .filter(binding -> binding.getTgid().equals(tgid) && binding.getBindingType() == BindingType.EXCLUDE)
                .findFirst()
                .orElse(null);
        
        if (existingExcludeBinding != null) {
            // 如果存在排除绑定，删除排除绑定即可
            bindingRepository.deleteBinding(tgid, ugid);
            return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                    .tgid(tgid)
                    .terminalGroupName(terminalGroupName)
                    .operationType("CONVERTED_TO_INCLUDE")
                    .description("恢复权限")
                    .build();
        }
        
        // 检查是否已经存在包含绑定
        UserGroupTerminalGroupBinding existingIncludeBinding = currentBindings.stream()
                .filter(binding -> binding.getTgid().equals(tgid) && binding.getBindingType() == BindingType.INCLUDE)
                .findFirst()
                .orElse(null);
        
        if (existingIncludeBinding != null) {
            // 已经存在包含绑定，无需操作
            return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                    .tgid(tgid)
                    .terminalGroupName(terminalGroupName)
                    .operationType("NO_CHANGE")
                    .description("已有权限")
                    .build();
        }
        
        // 检查是否通过父组已经有权限
        boolean hasPermissionThroughParent = hasPermissionThroughParentGroup(tgid, ugid, currentBindings);
        
        if (hasPermissionThroughParent) {
            // 通过父组已有权限，无需创建新绑定
            return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                    .tgid(tgid)
                    .terminalGroupName(terminalGroupName)
                    .operationType("NO_CHANGE")
                    .description("已有权限")
                    .build();
        }
        
        // 创建新的包含绑定
        UserGroupTerminalGroupBinding newBinding = UserGroupTerminalGroupBinding.builder()
                .ugid(ugid)
                .tgid(tgid)
                .includeSub(false)  // 默认不包含子组
                .bindingType(BindingType.INCLUDE)
                .creatorId(operatorId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        
        bindingRepository.createBinding(newBinding);
        
        return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                .tgid(tgid)
                .terminalGroupName(terminalGroupName)
                .operationType("CREATED_INCLUDE")
                .description("添加权限")
                .build();
    }
    
    /**
     * 处理解绑操作
     */
    private BatchBindingOperationResultDTO.BindingOperationDetailDTO processUnbindOperation(
            Long ugid, Long tgid, List<UserGroupTerminalGroupBinding> currentBindings, Long operatorId) {
        
        TerminalGroup targetGroup = terminalGroupService.getTerminalGroupById(tgid);
        String terminalGroupName = targetGroup != null ? targetGroup.getName() : "未知终端组";
        
        // 检查是否存在直接的包含绑定
        UserGroupTerminalGroupBinding existingIncludeBinding = currentBindings.stream()
                .filter(binding -> binding.getTgid().equals(tgid) && binding.getBindingType() == BindingType.INCLUDE)
                .findFirst()
                .orElse(null);
        
        if (existingIncludeBinding != null) {
            // 存在直接绑定，删除即可
            bindingRepository.deleteBinding(tgid, ugid);
            return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                    .tgid(tgid)
                    .terminalGroupName(terminalGroupName)
                    .operationType("DELETED_INCLUDE")
                    .description("移除权限")
                    .build();
        }
        
        // 检查是否通过父组有权限
        boolean hasPermissionThroughParent = hasPermissionThroughParentGroup(tgid, ugid, currentBindings);
        
        if (hasPermissionThroughParent) {
            // 通过父组有权限，创建排除绑定
            UserGroupTerminalGroupBinding excludeBinding = UserGroupTerminalGroupBinding.builder()
                    .ugid(ugid)
                    .tgid(tgid)
                    .includeSub(false)
                    .bindingType(BindingType.EXCLUDE)
                    .creatorId(operatorId)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            
            bindingRepository.createBinding(excludeBinding);
            
            return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                    .tgid(tgid)
                    .terminalGroupName(terminalGroupName)
                    .operationType("CREATED_EXCLUDE")
                    .description("移除权限")
                    .build();
        }
        
        // 没有任何权限，无需操作
        return BatchBindingOperationResultDTO.BindingOperationDetailDTO.builder()
                .tgid(tgid)
                .terminalGroupName(terminalGroupName)
                .operationType("NO_CHANGE")
                .description("无权限关系")
                .build();
    }
    
    /**
     * 检查是否通过父组绑定有权限
     */
    private boolean hasPermissionThroughParentGroup(Long tgid, Long ugid, List<UserGroupTerminalGroupBinding> currentBindings) {
        TerminalGroup targetGroup = terminalGroupService.getTerminalGroupById(tgid);
        if (targetGroup == null) {
            return false;
        }
        
        // 检查是否有父组的包含绑定
        for (UserGroupTerminalGroupBinding binding : currentBindings) {
            if (binding.getBindingType() == BindingType.INCLUDE && binding.getIncludeSub()) {
                TerminalGroup bindingGroup = terminalGroupService.getTerminalGroupById(binding.getTgid());
                if (bindingGroup != null && isChildOf(targetGroup, bindingGroup)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}