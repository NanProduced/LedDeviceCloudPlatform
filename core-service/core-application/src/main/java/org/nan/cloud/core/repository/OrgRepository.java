package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Organization;

public interface OrgRepository {

    Organization createOrganization(Organization organization);

    boolean updateOrganization(Organization organization);

    boolean updateOrganizationRootGroup(Long oid, Long rootUgid, Long rootTgid);

    Organization getOrganizationById(Long oid);

    Integer getSuffixById(Long oid);

}
