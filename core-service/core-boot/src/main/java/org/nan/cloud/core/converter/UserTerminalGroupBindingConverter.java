package org.nan.cloud.core.converter;

import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.PermissionExpressionDTO;
import org.nan.cloud.core.DTO.PermissionExpressionResultDTO;
import org.nan.cloud.core.DTO.UserGroupPermissionStatusDTO;
import org.nan.cloud.core.api.DTO.req.PermissionExpressionRequest;
import org.nan.cloud.core.api.DTO.res.PermissionExpressionResponse;
import org.nan.cloud.core.api.DTO.res.UserGroupPermissionStatusResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserTerminalGroupBindingConverter {

    /**
     * 转换权限表达式请求为内部DTO
     */
    public PermissionExpressionDTO convertToPermissionExpressionDTO(PermissionExpressionRequest request, RequestUserInfo userInfo) {
        List<PermissionExpressionDTO.PermissionBindingDTO> permissionBindings = request.getPermissionBindings().stream()
                .map(binding -> {
                    PermissionExpressionDTO.PermissionBindingDTO bindingDTO = new PermissionExpressionDTO.PermissionBindingDTO();
                    bindingDTO.setTgid(binding.getTgid());
                    bindingDTO.setBindingType(binding.getBindingType());
                    bindingDTO.setIncludeChildren(binding.getIncludeChildren());
                    bindingDTO.setRemarks(binding.getRemarks());
                    return bindingDTO;
                })
                .collect(Collectors.toList());

        PermissionExpressionDTO internalRequest = new PermissionExpressionDTO();
        internalRequest.setUgid(request.getUgid());
        internalRequest.setPermissionBindings(permissionBindings);
        internalRequest.setDescription(request.getDescription());
        internalRequest.setEnableRedundancyOptimization(request.getEnableRedundancyOptimization());
        internalRequest.setOperatorId(userInfo.getUid());
        internalRequest.setOid(userInfo.getOid());

        return internalRequest;
    }

    /**
     * 转换权限表达式结果为响应对象
     */
    public PermissionExpressionResponse convertToPermissionExpressionResponse(PermissionExpressionResultDTO result) {
        PermissionExpressionResponse response = new PermissionExpressionResponse();
        response.setSuccess(result.getSuccess());
        response.setMessage(result.getMessage());
        response.setUgid(result.getUgid());
        response.setOperationTime(result.getOperationTime());

        // 转换统计信息
        if (result.getStatistics() != null) {
            PermissionExpressionResponse.OperationStatistics statistics = getOperationStatistics(result);
            response.setStatistics(statistics);
        }

        // 转换优化后的绑定列表
        if (result.getOptimizedBindings() != null) {
            List<PermissionExpressionResponse.OptimizedBinding> optimizedBindings = result.getOptimizedBindings().stream()
                    .map(binding -> {
                        PermissionExpressionResponse.OptimizedBinding optimizedBinding = new PermissionExpressionResponse.OptimizedBinding();
                        optimizedBinding.setTgid(binding.getTgid());
                        optimizedBinding.setTerminalGroupName(binding.getTerminalGroupName());
                        optimizedBinding.setBindingType(binding.getBindingType());
                        optimizedBinding.setIncludeChildren(binding.getIncludeChildren());
                        optimizedBinding.setDepth(binding.getDepth());
                        optimizedBinding.setParentTgid(binding.getParentTgid());
                        optimizedBinding.setOptimized(binding.getOptimized());
                        return optimizedBinding;
                    })
                    .collect(Collectors.toList());
            response.setOptimizedBindings(optimizedBindings);
        }

        // 转换操作详情
        if (result.getOperationDetails() != null) {
            List<PermissionExpressionResponse.OperationDetail> operationDetails = result.getOperationDetails().stream()
                    .map(detail -> {
                        PermissionExpressionResponse.OperationDetail operationDetail = new PermissionExpressionResponse.OperationDetail();
                        operationDetail.setTgid(detail.getTgid());
                        operationDetail.setTerminalGroupName(detail.getTerminalGroupName());
                        operationDetail.setOperationType(detail.getOperationType());
                        operationDetail.setOldBinding(detail.getOldBinding());
                        operationDetail.setNewBinding(detail.getNewBinding());
                        operationDetail.setReason(detail.getReason());
                        operationDetail.setSuccess(detail.getSuccess());
                        operationDetail.setErrorMessage(detail.getErrorMessage());
                        return operationDetail;
                    })
                    .collect(Collectors.toList());
            response.setOperationDetails(operationDetails);
        }

        return response;
    }

    private PermissionExpressionResponse.OperationStatistics getOperationStatistics(PermissionExpressionResultDTO result) {
        PermissionExpressionResponse.OperationStatistics statistics = new PermissionExpressionResponse.OperationStatistics();
        statistics.setOriginalCount(result.getStatistics().getOriginalCount());
        statistics.setOptimizedCount(result.getStatistics().getOptimizedCount());
        statistics.setRedundancyRemoved(result.getStatistics().getRedundancyRemoved());
        statistics.setAddedCount(result.getStatistics().getAddedCount());
        statistics.setUpdatedCount(result.getStatistics().getUpdatedCount());
        statistics.setDeletedCount(result.getStatistics().getDeletedCount());
        statistics.setOptimizationRatio(result.getStatistics().getOptimizationRatio());
        return statistics;
    }

    /**
     * 转换用户组权限状态为响应对象
     */
    public UserGroupPermissionStatusResponse convertToUserGroupPermissionStatusResponse(UserGroupPermissionStatusDTO result) {
        UserGroupPermissionStatusResponse response = new UserGroupPermissionStatusResponse();
        response.setUgid(result.getUgid());
        response.setUserGroupName(result.getUserGroupName());
        response.setLastUpdateTime(result.getLastUpdateTime());

        // 转换权限绑定状态列表
        if (result.getPermissionBindings() != null) {
            List<UserGroupPermissionStatusResponse.PermissionBindingStatus> permissionBindings = result.getPermissionBindings().stream()
                    .map(binding -> {
                        UserGroupPermissionStatusResponse.PermissionBindingStatus bindingStatus = new UserGroupPermissionStatusResponse.PermissionBindingStatus();
                        bindingStatus.setBindingId(binding.getBindingId());
                        bindingStatus.setTgid(binding.getTgid());
                        bindingStatus.setTerminalGroupName(binding.getTerminalGroupName());
                        bindingStatus.setTerminalGroupPath(binding.getTerminalGroupPath());
                        bindingStatus.setBindingType(binding.getBindingType());
                        bindingStatus.setIncludeChildren(binding.getIncludeChildren());
                        bindingStatus.setDepth(binding.getDepth());
                        bindingStatus.setParentTgid(binding.getParentTgid());
                        bindingStatus.setCreateTime(binding.getCreateTime());
                        bindingStatus.setUpdateTime(binding.getUpdateTime());
                        bindingStatus.setRemarks(binding.getRemarks());
                        return bindingStatus;
                    })
                    .collect(Collectors.toList());
            response.setPermissionBindings(permissionBindings);
        }

        // 转换统计信息
        if (result.getStatistics() != null) {
            UserGroupPermissionStatusResponse.BindingStatistics statistics = getBindingStatistics(result);
            response.setStatistics(statistics);
        }

        return response;
    }

    private UserGroupPermissionStatusResponse.BindingStatistics getBindingStatistics(UserGroupPermissionStatusDTO result) {
        UserGroupPermissionStatusResponse.BindingStatistics statistics = new UserGroupPermissionStatusResponse.BindingStatistics();
        statistics.setTotalBindings(result.getStatistics().getTotalBindings());
        statistics.setIncludeBindings(result.getStatistics().getIncludeBindings());
        statistics.setExcludeBindings(result.getStatistics().getExcludeBindings());
        statistics.setIncludeChildrenBindings(result.getStatistics().getIncludeChildrenBindings());
        statistics.setTotalCoveredTerminalGroups(result.getStatistics().getTotalCoveredTerminalGroups());
        statistics.setMaxDepth(result.getStatistics().getMaxDepth());
        return statistics;
    }

}
