package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.ProgramMaterialRef;
import org.nan.cloud.program.entity.ProgramMaterialRefDO;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProgramMaterialRefConverter {

    ProgramMaterialRefDO toDO(ProgramMaterialRef ref);

    ProgramMaterialRef toDomain(ProgramMaterialRefDO ref);

    List<ProgramMaterialRefDO> toDOs(List<ProgramMaterialRef> refs);

    List<ProgramMaterialRef> toDomains(List<ProgramMaterialRefDO> refs);
}

