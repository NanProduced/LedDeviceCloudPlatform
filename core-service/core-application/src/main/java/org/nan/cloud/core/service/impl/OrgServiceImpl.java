package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.RandomUtils;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.TerminalGroup;
import org.nan.cloud.core.domain.UserGroup;
import org.nan.cloud.core.exception.BusinessException;
import org.nan.cloud.core.repository.*;
import org.nan.cloud.core.service.OrgService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final OrgRepository orgRepository;

    private final UserGroupRepository userGroupRepository;

    private final TerminalGroupRepository terminalGroupRepository;

    private final UserGroupTerminalGroupBindingRepository  userGroupTerminalGroupBindingRepository;

    @Override
    @Transactional
    public Organization createOrg(CreateOrgDTO createOrgDTO, Long currentUid) {
        // 创建 organization
        Organization organization = Organization
                .builder()
                .name(createOrgDTO.getOrgName())
                .remark(createOrgDTO.getRemark())
                .creatorId(currentUid)
                .build();
        organization = tryCreateOrgWithSuffix(organization);
        Long oid = organization.getOid();
        log.info("ORG_CREATE===>Organization created with OID {}", oid);
        // 创建组织根用户组
        UserGroup rootUserGroup = userGroupRepository.createUserGroup(UserGroup
                        .builder()
                        .name(createOrgDTO.getOrgName())
                        .description(createOrgDTO.getRemark())
                        .oid(oid)
                        .parent(0L)
                        .path("")
                        .ugType(0)
                        .creatorId(currentUid)
                        .build());
        organization.setRootUgid(rootUserGroup.getUgid());
        log.info("ORG_CREATE===>Organization root user group with id {}", organization.getRootUgid());
        // 创建组织根终端组
        TerminalGroup rootTerminalGroup = terminalGroupRepository.createTerminalGroup(TerminalGroup
                        .builder()
                        .name(createOrgDTO.getOrgName())
                        .description(createOrgDTO.getRemark())
                        .oid(oid)
                        .parent(0L)
                        .path("")
                        .tgType(0)
                        .creatorId(currentUid)
                        .build());
        organization.setRootTgid(rootTerminalGroup.getTgid());
        log.info("ORG_CREATE===>Organization root terminal group with id {}", organization.getRootTgid());
        if (!orgRepository.updateOrganization(organization)) throw new BusinessException(ExceptionEnum.UPDATE_FAILED, "update root group failed");
        userGroupTerminalGroupBindingRepository.initOrgBindings(oid, organization.getRootUgid(), organization.getRootTgid());
        log.info("ORG_CREATE===>Organization root user&terminal group binding successfully");
        return organization;
    }

    @Override
    public Organization getOrgByOid(Long oid) {
        return orgRepository.getOrganizationById(oid);
    }

    @Override
    public Integer getSuffixById(Long oid) {
        return orgRepository.getSuffixById(oid);
    }

    /**
     * 创建组织及后缀
     * @param dto
     * @return
     */
    private Organization tryCreateOrgWithSuffix(Organization dto) {
        int attempt = 0;
        while (true) {
            attempt++;
            dto.setSuffix(RandomUtils.random5Digits());
            try {
                return orgRepository.createOrganization(dto);
            } catch (DuplicateKeyException ex) {
                if (attempt >= 5) {
                    throw new BusinessException(ex, "Organizational suffixes conflict multiple times, please try again later");
                }
            }
        }
    }
}
