package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.BatchMaterialMetadataRequest;
import org.nan.cloud.core.api.DTO.req.CreateFolderRequest;
import org.nan.cloud.core.api.DTO.res.BatchMaterialMetadataResponse;
import org.nan.cloud.core.api.DTO.res.ListMaterialResponse;
import org.nan.cloud.core.api.DTO.res.ListSharedMaterialResponse;
import org.nan.cloud.core.api.DTO.res.MaterialMetadataItem;
import org.nan.cloud.core.api.DTO.res.MaterialNodeTreeResponse;
import org.nan.cloud.core.api.MaterialApi;
import org.nan.cloud.core.facade.FolderFacade;
import org.nan.cloud.core.facade.MaterialFacade;
import org.nan.cloud.core.service.MaterialMetadataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Material(素材资源控制器)", description = "素材/文件夹相关所有操作")
@RestController
@RequiredArgsConstructor
public class MaterialController implements MaterialApi {

    private final FolderFacade folderFacade;
    private final MaterialFacade materialFacade;
    private final MaterialMetadataService materialMetadataService;

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

    // ========================= 素材元数据批量查询接口 =========================

    @Operation(
            summary = "批量查询素材元数据",
            description = "根据素材ID列表批量获取详细元数据信息，专为节目编辑器设计的高性能接口",
            tags = {"素材元数据"}
    )
    @PostMapping("/metadata/batch")
    public BatchMaterialMetadataResponse batchGetMaterialMetadata(
            @RequestBody @Valid BatchMaterialMetadataRequest request) {
        
        return materialMetadataService.batchGetMaterialMetadata(request);
    }

    @Operation(
            summary = "查询单个素材元数据",
            description = "根据素材ID获取详细元数据信息，包含图片/视频专属信息",
            tags = {"素材元数据"}
    )
    @GetMapping("/metadata/{materialId}")
    public MaterialMetadataItem getMaterialMetadata(
            @Parameter(description = "素材ID", required = true) @PathVariable Long materialId,
            @Parameter(description = "是否包含缩略图信息") @RequestParam(defaultValue = "true") Boolean includeThumbnails,
            @Parameter(description = "是否包含基础文件信息") @RequestParam(defaultValue = "true") Boolean includeBasicInfo,
            @Parameter(description = "是否包含图片元数据") @RequestParam(defaultValue = "true") Boolean includeImageMetadata,
            @Parameter(description = "是否包含视频元数据") @RequestParam(defaultValue = "true") Boolean includeVideoMetadata) {
        
        return materialMetadataService.getMaterialMetadata(
            materialId, includeThumbnails, includeBasicInfo, includeImageMetadata, includeVideoMetadata
        );
    }

    @Operation(
            summary = "检查素材元数据存在性",
            description = "快速检查素材是否存在元数据，用于前端状态判断",
            tags = {"素材元数据"}
    )
    @GetMapping("/metadata/{materialId}/exists")
    public Boolean checkMetadataExists(
            @Parameter(description = "素材ID", required = true) @PathVariable Long materialId) {
        
        return materialMetadataService.hasMetadata(materialId);
    }

    @Operation(
            summary = "获取元数据查询性能统计",
            description = "用于监控和优化查询性能的统计信息",
            tags = {"素材元数据"}
    )
    @GetMapping("/metadata/performance-stats")
    public String getMetadataPerformanceStats() {
        
        return materialMetadataService.getPerformanceStats();
    }
}
