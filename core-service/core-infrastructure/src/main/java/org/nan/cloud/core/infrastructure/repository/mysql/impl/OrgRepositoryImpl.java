package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.OrganizationDO;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.OrgMapper;
import org.nan.cloud.core.repository.OrgRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class OrgRepositoryImpl implements OrgRepository {

    private final OrgMapper orgMapper;

    private final CommonConverter commonConverter;

    @Override
    public Organization createOrganization(Organization organization) {
        final OrganizationDO organizationDO = commonConverter.organization2OrganizationDO(organization);
        final LocalDateTime now = LocalDateTime.now();
        organizationDO.setCreateTime(now);
        organizationDO.setUpdateTime(now);
        orgMapper.insert(organizationDO);
        return commonConverter.organizationDO2Organization(organizationDO);
    }

    @Override
    public boolean updateOrganization(Organization organization) {
        final OrganizationDO organizationDO = commonConverter.organization2OrganizationDO(organization);
        organizationDO.setUpdateTime(LocalDateTime.now());
        return orgMapper.updateById(organizationDO) == 1;
    }

    @Override
    public boolean updateOrganizationRootGroup(Long oid, Long rootUgid, Long rootTgid) {
        return orgMapper.update(new LambdaUpdateWrapper<OrganizationDO>()
                .set(OrganizationDO::getRootUgid, rootUgid)
                .set(OrganizationDO::getRootTgid, rootTgid)
                .eq(OrganizationDO::getOid, oid)) == 1;
    }

    @Override
    public Organization getOrganizationById(Long oid) {
        OrganizationDO orgDO = orgMapper.selectById(oid);
        return orgDO == null ? null : commonConverter.organizationDO2Organization(orgDO);
    }

    @Override
    public Integer getSuffixById(Long oid) {
        return orgMapper.selectOne(new LambdaQueryWrapper<OrganizationDO>()
                .select(OrganizationDO::getSuffix)
                .eq(OrganizationDO::getOid, oid))
                .getSuffix();
    }
}
