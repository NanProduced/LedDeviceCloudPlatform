package org.nan.cloud.core.infrastructure.repository.mysql.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.domain.Folder;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.domain.MaterialShareRel;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.FolderDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialShareRelDO;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface MaterialConverter {

    // === Folder 转换 ===
    Folder toFolder(FolderDO folderDO);

    FolderDO toFolderDO(Folder folder);

    List<Folder> toFolders(List<FolderDO> folderDOS);

    List<FolderDO> toFolderDOs(List<FolderDO> folderDOS);

    // === Material 转换 ===
    Material toMaterial(MaterialDO materialDO);

    MaterialDO toMaterialDO(Material material);

    List<Material> toMaterials(List<MaterialDO> materialDOS);

    List<MaterialDO> toMaterialDOs(List<Material> materials);

    // === MaterialShareRel 转换 ===
    MaterialShareRel toMaterialShareRel(MaterialShareRelDO materialShareRelDO);

    MaterialShareRelDO toMaterialShareRelDO(MaterialShareRel materialShareRel);

    List<MaterialShareRel> toMaterialShareRels(List<MaterialShareRelDO> materialShareRelDOS);

    List<MaterialShareRelDO> toMaterialShareRelDOs(List<MaterialShareRel> materialShareRels);
}
