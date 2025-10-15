package org.nan.cloud.program.vsn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 文件源配置 - 对应VSN <filesource>
 * 这是VSN格式中的核心配置，用于指定素材文件的位置和信息
 */
@Data
public class FileSource {
    
    /**
     * 是否相对路径 - 对应VSN <isrelative>
     * "0"=绝对路径, "1"=相对路径
     */
    @JsonProperty("isRelative")
    @Field("is_relative")
    private String isRelative;
    
    /**
     * 文件路径 - 对应VSN <filepath>
     */
    @JsonProperty("filePath")
    @Field("file_path")
    private String filePath;
    
    /**
     * 文件MD5码 - 对应VSN <MD5>
     */
    @JsonProperty("md5")
    @Field("md5")
    private String md5;
    
    /**
     * 原始文件名 - 对应VSN <originName>
     */
    @JsonProperty("originName")
    @Field("origin_name")
    private String originName;
    
    /**
     * 文件转换路径 - 对应VSN <convertPath>
     */
    @JsonProperty("convertPath")
    @Field("convert_path")
    private String convertPath;
}