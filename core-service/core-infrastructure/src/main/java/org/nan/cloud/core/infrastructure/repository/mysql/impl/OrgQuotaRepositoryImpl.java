package org.nan.cloud.core.infrastructure.repository.mysql.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.OrgQuota;
import org.nan.cloud.core.infrastructure.repository.mysql.converter.CommonConverter;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.OrgQuotaMapper;
import org.nan.cloud.core.repository.OrgQuotaRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrgQuotaRepositoryImpl implements OrgQuotaRepository {

    private final OrgQuotaMapper orgQuotaMapper;

    private final CommonConverter commonConverter;

    @Override
    public OrgQuota findByOrgId(Long orgId) {
        return commonConverter.toOrgQuota(orgQuotaMapper.selectById(orgId));
    }
}
