package org.nan.cloud.core.domain;

import org.nan.cloud.core.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.core.enums.ProgramStatusEnum;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 节目基础信息实体类
 * 对应MySQL表 material_program
 */
public class Program {

    /**
     * 节目ID（主键）
     */
    private String programId;

    /**
     * 节目名称
     */
    private String programName;
    
    /**
     * 节目描述
     */
    private String description;

    // 节目规格信息
    /**
     * 画布宽度
     */
    private Integer canvasWidth;
    
    /**
     * 画布高度
     */
    private Integer canvasHeight;
    
    /**
     * 设备类型
     */
    private String deviceType;

    // 权限主要是依据所属用户组来判断

    /**
     * 所属组织ID
     */
    private Long oid;

    /**
     * 所属用户组ID
     */
    private Long ugid;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 更新者ID
     */
    private Long updaterId;

    /**
     * 节目详情数据存储在MongoDB
     * 这里是mongoDB文档的objectId
     */
    private String programContentId;

    /**
     * 生成的vsn文件Id
     */
    private String vsnFileId;

    /**
     * 缩略图路径
     */
    private String thumbnailPath;
    
    /**
     * 预览视频路径
     */
    private String previewVideoPath;

    // 版本控制
    /**
     * 版本号，节目创建时为1
     * 每次进行编辑版本号+1
     * 生成一个新的版本 - 创建者为原始节目作者，修改者为编辑者
     */
    private Integer version;

    /**
     * 原始节目Id
     */
    private String sourceProgramId;

    /**
     * 是否是原始节目
     */
    private Boolean sourceProgram;

    /**
     * 节目审核状态 - 每个组织的节目策略不同
     * 组织配置完节目策略后，根据配置详情来判断创建节目后是否进入审核流程
     */
    private ProgramApprovalStatusEnum approvalStatus;

    /**
     * 节目状态
     * 如果组织开启审核流程 - 需要通过审核后才会有这个状态
     */
    private ProgramStatusEnum programStatus;

    /**
     * 使用计数 - 发布计数
     */
    private Integer usageCount;
    
    /**
     * 最后发布时间
     */
    private LocalDateTime lastDeployTime;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 标签集合
     */
    private Set<String> tags;
    
    /**
     * 支持的设备类型集合
     */
    private Set<String> supportedDeviceTypes;
    
    /**
     * 最低播放器版本
     */
    private String minPlayerVersion;

    // 时间戳
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Getters and Setters
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCanvasWidth() { return canvasWidth; }
    public void setCanvasWidth(Integer canvasWidth) { this.canvasWidth = canvasWidth; }

    public Integer getCanvasHeight() { return canvasHeight; }
    public void setCanvasHeight(Integer canvasHeight) { this.canvasHeight = canvasHeight; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public Long getOid() { return oid; }
    public void setOid(Long oid) { this.oid = oid; }

    public Long getUgid() { return ugid; }
    public void setUgid(Long ugid) { this.ugid = ugid; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public Long getUpdaterId() { return updaterId; }
    public void setUpdaterId(Long updaterId) { this.updaterId = updaterId; }

    public String getProgramContentId() { return programContentId; }
    public void setProgramContentId(String programContentId) { this.programContentId = programContentId; }

    public String getVsnFileId() { return vsnFileId; }
    public void setVsnFileId(String vsnFileId) { this.vsnFileId = vsnFileId; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public String getPreviewVideoPath() { return previewVideoPath; }
    public void setPreviewVideoPath(String previewVideoPath) { this.previewVideoPath = previewVideoPath; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getSourceProgramId() { return sourceProgramId; }
    public void setSourceProgramId(String sourceProgramId) { this.sourceProgramId = sourceProgramId; }

    public Boolean getSourceProgram() { return sourceProgram; }
    public void setSourceProgram(Boolean sourceProgram) { this.sourceProgram = sourceProgram; }

    public ProgramApprovalStatusEnum getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ProgramApprovalStatusEnum approvalStatus) { this.approvalStatus = approvalStatus; }

    public ProgramStatusEnum getProgramStatus() { return programStatus; }
    public void setProgramStatus(ProgramStatusEnum programStatus) { this.programStatus = programStatus; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public LocalDateTime getLastDeployTime() { return lastDeployTime; }
    public void setLastDeployTime(LocalDateTime lastDeployTime) { this.lastDeployTime = lastDeployTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public Set<String> getSupportedDeviceTypes() { return supportedDeviceTypes; }
    public void setSupportedDeviceTypes(Set<String> supportedDeviceTypes) { this.supportedDeviceTypes = supportedDeviceTypes; }

    public String getMinPlayerVersion() { return minPlayerVersion; }
    public void setMinPlayerVersion(String minPlayerVersion) { this.minPlayerVersion = minPlayerVersion; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
