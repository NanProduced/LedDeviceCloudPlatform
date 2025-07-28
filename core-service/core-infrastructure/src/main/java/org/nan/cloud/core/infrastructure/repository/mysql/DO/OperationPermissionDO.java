package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("operation_permission")
@Data
public class OperationPermissionDO {

    @TableId(value = "operation_permission_id", type = IdType.AUTO)
    private Long operationPermissionId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("operation_type")
    private String operationType;

    @TableField("create_time")
    private LocalDateTime createTime;

}
