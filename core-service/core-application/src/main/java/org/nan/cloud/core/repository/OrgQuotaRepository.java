package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.OrgQuota;

public interface OrgQuotaRepository {

    OrgQuota findByOrgId(Long orgId);
}
