package org.nan.cloud.core.domain;

import lombok.Data;
import org.nan.cloud.core.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.core.enums.ProgramStatusEnum;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 节目基础信息领域类
 */
@Data
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


    // 时间戳
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
