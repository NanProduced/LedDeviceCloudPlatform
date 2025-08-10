package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.BatchMaterialMetadataRequest;
import org.nan.cloud.core.api.DTO.req.CreateFolderRequest;
import org.nan.cloud.core.api.DTO.res.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface MaterialApi {

    String prefix = "/material";

    @PostMapping(prefix + "/folder/create")
    void createFolder(CreateFolderRequest request);

    @GetMapping(prefix + "/tree/init")
    MaterialNodeTreeResponse initMaterialStructTree();

    @GetMapping(prefix + "/get/all")
    List<ListMaterialResponse> listAllVisibleMaterials();

    @GetMapping(prefix + "/list/user")
    List<ListMaterialResponse> listUserMaterials(Long ugid, Long fid, Boolean includeSub);

    @GetMapping(prefix + "/list/public")
    List<ListMaterialResponse> listPublicMaterials(Long fid, boolean includeSub);

    @GetMapping(prefix + "/list/shared")
    List<ListSharedMaterialResponse> listSharedMaterials(Long fid, boolean includeSub);

    @PostMapping(prefix + "/metadata/batch")
    BatchMaterialMetadataResponse batchGetMaterialMetadata(BatchMaterialMetadataRequest request);

    @GetMapping(prefix + "/metadata/{materialId}")
    MaterialMetadataItem getMaterialMetadata(
            @PathVariable Long materialId,
            Boolean includeThumbnails,
            Boolean includeBasicInfo,
            Boolean includeImageMetadata,
            Boolean includeVideoMetadata);

    @GetMapping(prefix + "/metadata/{materialId}/exists")
    Boolean checkMetadataExists(@PathVariable Long materialId);

    @GetMapping(prefix + "/metadata/performance-stats")
    String getMetadataPerformanceStats();


}
