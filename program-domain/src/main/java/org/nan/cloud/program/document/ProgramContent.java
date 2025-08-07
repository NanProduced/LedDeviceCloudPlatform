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
 * 其中vsnPrograms对应VSN <Programs>结构，存储节目的完整内容
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
    private Long programId;
    
    /**
     * 节目版本号
     */
    @JsonProperty("version")
    @Field("version")
    private String  version;

    /**
     * 对应VSN文件结构 - 对应VSN <Programs>
     */
    @JsonProperty("vsn_programs")
    @Field("vsn_programs")
    private List<VsnProgram> vsnPrograms;
    
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