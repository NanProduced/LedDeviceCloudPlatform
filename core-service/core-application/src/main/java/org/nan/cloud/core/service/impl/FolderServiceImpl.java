package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.core.DTO.CreateFolderDTO;
import org.nan.cloud.core.domain.Folder;
import org.nan.cloud.core.repository.FolderRepository;
import org.nan.cloud.core.service.FolderService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;

    @Override
    public void createFolderInFolder(CreateFolderDTO dto) {
        Folder parent = folderRepository.getFolderById(dto.getTargetFid());
        ExceptionEnum.HAS_DUPLICATE_FOLDER_NAME.throwIf(folderRepository.ifHasSameFolderNameInFolder(parent.getFid(), dto.getFolderName()));
        Folder folder = Folder.builder()
                .folderName(dto.getFolderName())
                .description(dto.getDescription())
                .oid(dto.getOid())
                .ugid(parent.getUgid())
                .parent(parent.getFid())
                .path(parent.getPath())
                .creatorId(dto.getUid())
                .build();
        folderRepository.createFolder(folder);
    }

    @Override
    public void createFolderInUserGroup(CreateFolderDTO dto) {
        ExceptionEnum.HAS_DUPLICATE_FOLDER_NAME.throwIf(folderRepository.ifHasSameFolderNameInUserGroup(dto.getTargetUgid(), dto.getFolderName()));
        Folder folder = Folder.builder()
                .folderName(dto.getFolderName())
                .description(dto.getDescription())
                .oid(dto.getOid())
                .ugid(dto.getTargetUgid())
                .creatorId(dto.getUid())
                .build();
        folderRepository.createFolder(folder);
    }
}
