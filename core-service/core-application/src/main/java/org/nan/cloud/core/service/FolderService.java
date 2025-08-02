package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.CreateFolderDTO;
import org.nan.cloud.core.domain.Folder;

public interface FolderService {

    void createFolderInFolder(CreateFolderDTO dto);

    void createFolderInUserGroup(CreateFolderDTO dto);
}
