package org.nan.cloud.common.basic.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询请求 DTO
 *
 * @param <Q> 查询条件类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequestDTO<Q> implements Serializable {
    @Serial
    private static final long serialVersionUID = 5028574726204796075L;

    /** 当前页码，从 1 开始 */
    @Builder.Default
    private int pageNum = 1;

    /** 每页条数 */
    @Builder.Default
    private int pageSize = 10;

    /** 排序字段，例如 "createTime" */
    private String sortField;

    /** 排序方向，可选 "ASC" 或 "DESC" */
    @Builder.Default
    private String sortOrder = "ASC";

    /** 额外的查询条件 */
    private Q params;
}
