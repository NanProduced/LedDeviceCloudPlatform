package org.nan.cloud.program.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * vsn program封装 - 对应VSN <Program>
 */
@Data
public class VsnProgram {

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
}
