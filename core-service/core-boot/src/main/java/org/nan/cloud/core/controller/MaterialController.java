package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.CreateFolderRequest;
import org.nan.cloud.core.api.DTO.res.ListMaterialResponse;
import org.nan.cloud.core.api.DTO.res.ListSharedMaterialResponse;
import org.nan.cloud.core.api.DTO.res.MaterialNodeTreeResponse;
import org.nan.cloud.core.api.MaterialApi;
import org.nan.cloud.core.facade.FolderFacade;
import org.nan.cloud.core.facade.MaterialFacade;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Material(素材资源控制器)", description = "素材/文件夹相关所有操作")
@RestController
@RequiredArgsConstructor
public class MaterialController implements MaterialApi {

    private final FolderFacade folderFacade;
    private final MaterialFacade materialFacade;

    @Operation(
            summary = "创建文件夹",
            description = "在对应路径下创建文件夹",
            tags = {"素材管理"}
    )
    @Override
    public void createFolder(@RequestBody @Validated CreateFolderRequest request) {
        folderFacade.createFolder(request);
    }

    @Operation(
            summary = "素材文件结构树初始化",
            description = "初始化素材管理左侧树形结构",
            tags = {"素材管理"}
    )
    @Override
    public MaterialNodeTreeResponse initMaterialStructTree() {
        return materialFacade.initMaterialStructTree();
    }

    @Operation(
            summary = "查询全部素材",
            description = "快速查询全部当前用户可见素材",
            tags = {"素材管理"}
    )
    @Override
    public List<ListMaterialResponse> listAllVisibleMaterials() {
        return materialFacade.listAllVisibleMaterials();
    }

    /**
     *
     * @param ugid 查询某个用户组下素材
     * @param fid 查询用户组某个文件夹下素材
     * @param includeSub 是否包含子组/子文件夹
     * @return
     */
    @Operation(
            summary = "查询用户组素材",
            description = "根据用户组Id,用户组下文件夹Id查询素材",
            tags = {"素材管理"},
            parameters = {

            }
    )
    @Override
    public List<ListMaterialResponse> listUserMaterials(@RequestParam(value = "ugid", required = false) Long ugid,
                                                        @RequestParam(value = "fid", required = false) Long fid,
                                                        @RequestParam(value = "includeSub", defaultValue = "false") Boolean includeSub) {
        return materialFacade.listUserMaterials(ugid, fid, includeSub);
    }

    /**
     *
     * @param fid 查询公共资源组下某个文件夹下素材
     * @param includeSub 是否包含子文件夹
     * @return
     */
    @Operation(
            summary = "查询公共资源组素材",
            description = "查询公共资源组下素材",
            tags = "资源管理"
    )
    @Override
    public List<ListMaterialResponse> listPublicMaterials(@RequestParam(value = "fid", required = false) Long fid,
                                                          @RequestParam(value = "includeSub", defaultValue = "false") boolean includeSub) {
        return materialFacade.listPublicMaterials(fid, includeSub);
    }

    /**
     *
     * @param fid 分享文件夹中的文件夹Id
     * @param includeSub 是否包含子文件夹
     * @return
     */
    @Operation(
            summary = "查询由其他用户组分享的素材",
            description = "查询分享文件夹下素材",
            tags = "资源管理"
    )
    @Override
    public List<ListSharedMaterialResponse> listSharedMaterials(
            @RequestParam(value = "fid", required = false) Long fid,
            @RequestParam(value = "includeSub", defaultValue = "false") boolean includeSub) {
        return materialFacade.listSharedMaterials(fid, includeSub);
    }
}
