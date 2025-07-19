package org.nan.cloud.core.casbin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.PermissionDenyRequest;
import org.nan.cloud.core.service.PermissionDenyService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PermissionDeny(权限拒绝管理)", description = "权限拒绝相关操作")
@RestController
@RequestMapping("/permission-deny")
@RequiredArgsConstructor
@Validated
public class PermissionDenyController {
    
    private final PermissionDenyService permissionDenyService;
    
    @Operation(
        summary = "添加拒绝权限",
        description = "为指定用户或角色添加拒绝权限"
    )
    @PostMapping("/add")
    public void addDenyPermission(@Validated @RequestBody PermissionDenyRequest request) {
        if ("USER".equals(request.getTargetType())) {
            if (request.getDurationHours() != null) {
                // 临时禁用
                permissionDenyService.temporaryDenyUser(
                    request.getTargetId(),
                    request.getOrgId(),
                    request.getUrl(),
                    request.getMethod(),
                    request.getDurationHours(),
                    request.getReason()
                );
            } else {
                // 永久禁用
                permissionDenyService.addUserDenyPermission(
                    request.getTargetId(),
                    request.getOrgId(),
                    request.getUrl(),
                    request.getMethod(),
                    request.getReason()
                );
            }
        } else if ("ROLE".equals(request.getTargetType())) {
            permissionDenyService.addRoleDenyPermission(
                request.getTargetId(),
                request.getOrgId(),
                request.getUrl(),
                request.getMethod(),
                request.getReason()
            );
        }
    }
    
    @Operation(
        summary = "移除拒绝权限",
        description = "移除指定用户或角色的拒绝权限"
    )
    @PostMapping("/remove")
    public void removeDenyPermission(@Validated @RequestBody PermissionDenyRequest request) {
        if ("USER".equals(request.getTargetType())) {
            permissionDenyService.removeUserDenyPermission(
                request.getTargetId(),
                request.getOrgId(),
                request.getUrl(),
                request.getMethod()
            );
        } else if ("ROLE".equals(request.getTargetType())) {
            permissionDenyService.removeRoleDenyPermission(
                request.getTargetId(),
                request.getOrgId(),
                request.getUrl(),
                request.getMethod()
            );
        }
    }
    
    @Operation(
        summary = "检查用户是否被拒绝",
        description = "检查用户是否被拒绝访问特定接口"
    )
    @GetMapping("/check")
    public boolean checkUserDenied(
            @RequestParam("userId") Long userId,
            @RequestParam("orgId") Long orgId,
            @RequestParam("url") String url,
            @RequestParam("method") String method) {
        return permissionDenyService.isUserDenied(userId, orgId, url, method);
    }
    
    @Operation(
        summary = "移除用户所有拒绝权限",
        description = "移除用户在指定组织的所有拒绝权限"
    )
    @PostMapping("/remove-all")
    public void removeAllUserDenyPermissions(
            @RequestParam("userId") Long userId,
            @RequestParam("orgId") Long orgId) {
        permissionDenyService.removeAllUserDenyPermissions(userId, orgId);
    }
}