package org.nan.cloud.core.converter;

import javax.annotation.processing.Generated;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.DTO.CreateOrgVO;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-20T02:53:19+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class OrgConverterImpl implements OrgConverter {

    @Override
    public CreateOrgDTO createOrgRequest2CreateOrgDTO(CreateOrgRequest request) {
        if ( request == null ) {
            return null;
        }

        CreateOrgDTO createOrgDTO = new CreateOrgDTO();

        if ( request.getOrgName() != null ) {
            createOrgDTO.setOrgName( request.getOrgName() );
        }
        if ( request.getRemark() != null ) {
            createOrgDTO.setRemark( request.getRemark() );
        }
        if ( request.getManagerName() != null ) {
            createOrgDTO.setManagerName( request.getManagerName() );
        }
        if ( request.getEmail() != null ) {
            createOrgDTO.setEmail( request.getEmail() );
        }
        if ( request.getPhone() != null ) {
            createOrgDTO.setPhone( request.getPhone() );
        }

        return createOrgDTO;
    }

    @Override
    public CreateOrgResponse createOrgResult2CreateOrgResponse(CreateOrgVO result) {
        if ( result == null ) {
            return null;
        }

        CreateOrgResponse.CreateOrgResponseBuilder createOrgResponse = CreateOrgResponse.builder();

        if ( result.getOid() != null ) {
            createOrgResponse.oid( result.getOid() );
        }
        if ( result.getOrgName() != null ) {
            createOrgResponse.orgName( result.getOrgName() );
        }
        if ( result.getSuffix() != null ) {
            createOrgResponse.suffix( result.getSuffix() );
        }
        if ( result.getUid() != null ) {
            createOrgResponse.uid( result.getUid() );
        }
        if ( result.getUsername() != null ) {
            createOrgResponse.username( result.getUsername() );
        }

        return createOrgResponse.build();
    }
}
