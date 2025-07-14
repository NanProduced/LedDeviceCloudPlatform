package org.nan.cloud.common.basic.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * 通用映射：将当前 PageVO<T> 转为 PageVO<R>
     *
     * @param mapper 将 T 映射为 R 的函数
     * @param <R>    目标类型
     * @return 映射后的 PageVO<R>
     */
    public <R> PageVO<R> map(Function<? super T, ? extends R> mapper) {
        PageVO<R> result = new PageVO<>();
        result.setPageNum(this.pageNum);
        result.setPageSize(this.pageSize);
        result.setTotal(this.total);
        // 转换 records 列表
        result.setRecords(
                this.records.stream()
                        .map(mapper)
                        .collect(Collectors.toList())
        );
        // 重新计算 totalPages、hasNext、hasPrevious
        result.calculate();
        return result;
    }

    /**
     * 复制分页元信息到新 PageVO，同时使用外部提供的 records 列表
     *
     * @param newRecords 前端或者其他层已经准备好的 List<R>
     * @param <R>        目标记录类型
     * @return 包含相同分页信息、但 records 被替换为 newRecords 的新 PageVO<R>
     */
    public <R> PageVO<R> withRecords(List<R> newRecords) {
        return PageVO.<R>builder()
                .pageNum(this.pageNum)
                .pageSize(this.pageSize)
                .total(this.total)
                .totalPages(this.totalPages)
                .records(newRecords)
                .hasNext(this.hasNext)
                .hasPrevious(this.hasPrevious)
                .build();
    }

}
