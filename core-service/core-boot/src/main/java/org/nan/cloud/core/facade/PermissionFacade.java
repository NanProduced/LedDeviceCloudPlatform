package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.res.OperationPermissionResponse;
import org.nan.cloud.core.domain.OperationPermission;
import org.nan.cloud.core.enums.UserTypeEnum;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionFacade {

    private final RoleAndPermissionService roleAndPermissionService;

    public Map<String, List<OperationPermissionResponse>> getCurrentUserOperations() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        List<OperationPermission> operations;
        if (requestUser.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            operations = roleAndPermissionService.getAllOperations();
        }
        else {
            operations = roleAndPermissionService.getOperationPermissionByUid(requestUser.getUid());
        }
        return operations.stream().map(o ->
                new OperationPermissionResponse(o.getOperationPermissionId(), o.getName(), o.getDescription(), o.getOperationType()))
                .collect(Collectors.groupingBy(OperationPermissionResponse::getOperationType));
    }
}
