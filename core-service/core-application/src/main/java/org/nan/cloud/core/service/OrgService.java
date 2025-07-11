package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.domain.Organization;

public interface OrgService {

    Organization createOrg(CreateOrgDTO createOrgDTO, Long currentUid);

    Organization getOrgById(Long oid);
}
