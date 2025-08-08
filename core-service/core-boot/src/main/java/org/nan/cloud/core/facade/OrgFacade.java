package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.nan.cloud.core.api.DTO.req.QuotaCheckRequest;
import org.nan.cloud.core.converter.OrgConverter;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.infrastructure.repository.enums.SystemRolesRelEnums;
import org.nan.cloud.core.service.OrgService;
import org.nan.cloud.core.service.PermissionEventPublisher;
import org.nan.cloud.core.service.QuotaService;
import org.nan.cloud.core.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class OrgFacade {

    private final OrgService orgService;
    private final UserService userService;
    private final OrgConverter orgConverter;
    private final PermissionEventPublisher  permissionEventPublisher;
    private final QuotaService quotaService;

    /**
     * 组织创建用例：
     * 1. 请求 → DTO
     * 2. 密码生成 + 加密
     * 3. 调用 application 模块的 service
     * 4. 结果 → 响应
     */
    @Transactional(rollbackFor =  Exception.class)
    public CreateOrgResponse createOrg(CreateOrgRequest req) {
        // 系统管理员
        ExceptionEnum.PERMISSION_DENIED.throwIf(!InvocationContextHolder.ifOrgManager());
        final Long currentUId = InvocationContextHolder.getCurrentUId();
        CreateOrgDTO dto = orgConverter.createOrgRequest2CreateOrgDTO(req);
        // 创建组织
        final Organization organization = orgService.createOrg(dto, currentUId);
        dto.fillOrgInfo(organization);
        // 创建组织管理员账户
        String initPsw = PasswordUtils.generatePassword(10);
        String encodePsw = PasswordUtils.encodeByBCrypt(initPsw);
        dto.setManagerPsw(encodePsw);
        final User orgManagerUser = userService.createOrgManagerUser(dto);
        // 分配组织管理员角色
        permissionEventPublisher.publishAddUserAndRoleRelEvent(orgManagerUser.getUid(), organization.getOid(), Collections.singletonList(SystemRolesRelEnums.ORG_MANAGER.getRid()));
        return CreateOrgResponse.builder()
                .oid(organization.getOid())
                .orgName(organization.getName())
                .suffix(organization.getSuffix())
                .uid(orgManagerUser.getUid())
                .password(initPsw)
                .username(orgManagerUser.getUsername())
                .build();
    }

    /**
     * 校验存储空间
     * @param request
     * @return
     */
    public Boolean checkOrgQuota(QuotaCheckRequest request) {

        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();

        return quotaService.checkQuotaAllow(requestUser.getOid(), request.getBytes(), request.getFiles());
    }
}
