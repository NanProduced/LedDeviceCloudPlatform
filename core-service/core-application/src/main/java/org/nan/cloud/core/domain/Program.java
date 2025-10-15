package org.nan.cloud.core.domain;

import lombok.Data;
import org.nan.cloud.program.enums.ProgramApprovalStatusEnum;
import org.nan.cloud.program.enums.ProgramStatusEnum;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;

import java.time.LocalDateTime;

/**
 * 节目基础信息领域类
 */
@Data
public class Program {

    /**
     * 节目ID（主键）
     */
    private Long id;

    /**
     * 节目名称
     */
    private String name;
    
    /**
     * 节目描述
     */
    private String description;

    // 节目规格信息
    /**
     * 画布宽度（像素）
     */
    private Integer width;
    
    /**
     * 画布高度（像素）
     */
    private Integer height;
    
    /**
     * 节目时长（毫秒）
     * 计算得出的总播放时长
     */
    private Long duration;
    
    /**
     * 节目缩略图URL
     */
    private String thumbnailUrl;

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
    private Long createdBy;

    /**
     * 更新者ID
     */
    private Long updatedBy;

    /**
     * 节目详情数据存储在MongoDB
     * 这里是mongoDB文档的objectId
     */
    private String programContentId;

    /**
     * VSN文件ID
     * 生成的VSN文件的唯一标识
     */
    private String vsnFileId;
    
    /**
     * VSN文件路径
     * VSN文件在文件系统或对象存储中的完整路径
     */
    private String vsnFilePath;
    
    /**
     * VSN生成状态
     * 跟踪VSN文件的生成进度和状态
     */
    private VsnGenerationStatusEnum vsnGenerationStatus;
    
    /**
     * VSN生成错误信息
     * 当VSN生成失败时，记录详细的错误信息用于排查
     */
    private String vsnGenerationError;
    
    /**
     * VSN文件大小
     * 生成的VSN文件字节大小，用于组织配额管理
     */
    private Long vsnFileSize;


    // 版本控制
    /**
     * 版本号
     * 从1开始，每次编辑创建新记录时递增
     */
    private Integer version;

    /**
     * 原始节目ID
     * 指向版本号为1的原始节目，用于版本链管理
     */
    private Long sourceProgramId;

    /**
     * 是否为原始节目
     * true=版本1的原始节目，false=基于原始节目的编辑版本
     */
    private Boolean isSourceProgram;

    /**
     * 节目审核状态 - 每个组织的节目策略不同
     * 组织配置完节目策略后，根据配置详情来判断创建节目后是否进入审核流程
     */
    private ProgramApprovalStatusEnum approvalStatus;

    /**
     * 节目状态
     * 如果组织开启审核流程 - 需要通过审核后才会有这个状态
     */
    private ProgramStatusEnum status;

    /**
     * 使用次数
     * 统计被引用的次数（不涉及具体发布逻辑）
     */
    private Integer usageCount;


    // 时间戳
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
