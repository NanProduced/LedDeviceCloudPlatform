package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.CreateFolderRequest;
import org.nan.cloud.core.api.DTO.res.ListMaterialResponse;
import org.nan.cloud.core.api.DTO.res.ListSharedMaterialResponse;
import org.nan.cloud.core.api.DTO.res.MaterialNodeTreeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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






}
