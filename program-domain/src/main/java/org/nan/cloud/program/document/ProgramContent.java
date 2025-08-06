package org.nan.cloud.program.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 节目详细内容 - MongoDB文档
 * 对应VSN <Program>结构，存储节目的完整内容
 */
@Data
@Document(collection = "program_contents")
public class ProgramContent {
    
    /**
     * MongoDB文档ID
     */
    @Id
    private String id;
    
    /**
     * 关联的节目ID（MySQL中的program表主键）
     */
    @JsonProperty("programId")
    @Field("program_id")
    private String programId;
    
    /**
     * 节目版本号
     */
    @JsonProperty("version")
    @Field("version")
    private String version;
    
    /**
     * 节目信息 - 对应VSN <Information>
     */
    @JsonProperty("information")
    @Field("information")
    private ProgramInformation information;
    
    /**
     * 节目页列表 - 对应VSN <Pages>
     */
    @JsonProperty("pages")
    @Field("pages")
    private List<ProgramPage> pages;
    
    /**
     * VSN节目ID - 对应VSN <programId>
     * 用于桶节目判断
     */
    @JsonProperty("vsnProgramId")
    @Field("vsn_program_id")
    private String vsnProgramId;
    
    /**
     * 是否为桶节目 - 对应VSN <isBucketProgram>
     */
    @JsonProperty("isBucketProgram")
    @Field("is_bucket_program")
    private Boolean isBucketProgram;
    
    /**
     * 原始前端数据
     * 存储前端传来的完整JSON数据，用于备份和调试
     */
    @JsonProperty("originalData")
    @Field("original_data")
    private Object originalData;
    
    /**
     * 生成的VSN XML内容
     * 缓存生成的VSN XML，避免重复生成
     */
    @JsonProperty("vsnXml")
    @Field("vsn_xml")
    private String vsnXml;
    
    /**
     * 创建时间
     */
    @JsonProperty("createdTime")
    @Field("created_time")
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @JsonProperty("updatedTime")
    @Field("updated_time")
    private LocalDateTime updatedTime;
    
    /**
     * 创建者用户ID
     */
    @JsonProperty("createdBy")
    @Field("created_by")
    private String createdBy;
    
    /**
     * 更新者用户ID
     */
    @JsonProperty("updatedBy")
    @Field("updated_by")
    private String updatedBy;
}