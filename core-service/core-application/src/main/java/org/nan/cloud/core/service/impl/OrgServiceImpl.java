package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.repository.OrgRepository;
import org.nan.cloud.core.service.OrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService {

    private final OrgRepository orgRepository;

    @Override
    @Transactional
    public Organization createOrg(CreateOrgDTO createOrgDTO, Long currentUid) {
        // 创建 organization
        Organization organization = Organization.builder()
                .name(createOrgDTO.getOrgName())
                .remark(createOrgDTO.getRemark())
                .creatorId(currentUid)
                .build();

        // 创建org root用户

        // 创建root 终端组
        // 创建root 用户组
        return null;
    }

}
