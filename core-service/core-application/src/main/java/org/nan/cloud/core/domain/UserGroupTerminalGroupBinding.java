package org.nan.cloud.core.domain;

import lombok.Builder;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;

import java.time.LocalDateTime;

/**
 * 用户组-终端组绑定实体
 */
@Data
@Builder
public class UserGroupTerminalGroupBinding {

    /** 绑定ID */
    private Long bindingId;

    /** 用户组ID */
    private Long ugid;

    /** 终端组ID */
    private Long tgid;

    /** 是否包含子终端组 */
    @Builder.Default
    private Boolean includeSub = false;
    
    /** 绑定类型 */
    @Builder.Default
    private BindingType bindingType = BindingType.INCLUDE;

    /** 组织ID */
    private Long oid;

    /** 创建者ID */
    private Long creatorId;

    /** 创建时间 */
    private LocalDateTime createTime;

    private Long updaterId;

    /** 更新时间 */
    private LocalDateTime updateTime;
}