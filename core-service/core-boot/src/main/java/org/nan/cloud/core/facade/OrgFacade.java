package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.auth.api.client.AuthClient;
import org.nan.cloud.common.basic.utils.PasswordUtils;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.nan.cloud.core.converter.OrgConverter;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.service.OrgService;
import org.nan.cloud.core.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrgFacade {

    private final OrgService orgService;
    private final UserService userService;
    private final OrgConverter orgConverter;
    private final AuthClient authClient;

    /**
     * 组织创建用例：
     * 1. 请求 → DTO
     * 2. 密码生成 + 加密
     * 3. 调用 application 模块的 service
     * 4. 结果 → 响应
     */
    @Transactional
    public CreateOrgResponse createOrg(CreateOrgRequest req) {
        final Long currentUId = InvocationContextHolder.getCurrentUId();
        CreateOrgDTO dto = orgConverter.createOrgRequest2CreateOrgDTO(req);
        final Organization organization = orgService.createOrg(dto, currentUId);
        dto.fillOrgInfo(organization);
        String initPsw = PasswordUtils.generatePassword(8);
        String encodePsw = authClient.encodePsw(initPsw);
        dto.setManagerPsw(encodePsw);
        final User orgManagerUser = userService.createOrgManagerUser(dto);
        return CreateOrgResponse.builder()
                .oid(organization.getOid())
                .orgName(organization.getName())
                .suffix(organization.getSuffix())
                .uid(orgManagerUser.getUid())
                .password(initPsw)
                .username(orgManagerUser.getUsername())
                .build();
    }
}
