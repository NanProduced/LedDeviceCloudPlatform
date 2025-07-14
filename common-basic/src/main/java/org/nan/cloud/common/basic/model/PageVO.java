package org.nan.cloud.common.basic.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页统一返回实体
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -1918440928078622128L;

    /** 当前页码，从 1 开始 */
    private int pageNum;

    /** 每页条数 */
    private int pageSize;

    /** 总记录数 */
    private long total;

    /** 总页数 = ceil(total / pageSize) */
    private int totalPages;

    /** 当前页数据列表 */
    @Builder.Default
    private List<T> records = Collections.emptyList();

    /** 是否有下一页 */
    @Builder.Default
    private boolean hasNext = false;

    /** 是否有上一页 */
    @Builder.Default
    private boolean hasPrevious = false;

    /**
     * 根据 pageNum、pageSize、total 以及 records 计算分页信息
     */
    public void calculate() {
        if (pageSize <= 0) {
            totalPages = 0;
        } else {
            totalPages = (int) ((total + pageSize - 1) / pageSize);
        }
        hasPrevious = pageNum > 1;
        hasNext = pageNum < totalPages;
    }
}
