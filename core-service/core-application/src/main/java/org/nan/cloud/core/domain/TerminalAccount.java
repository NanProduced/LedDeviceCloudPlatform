package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TerminalAccount {

    /**
     * 主键ID
     */
    private Long tid;

    /**
     * 设备名称
     */
    private String account;

    /**
     * 设备密码 - BCrypt加密
     */
    private String password;

    /**
     * 设备状态：ACTIVE-活跃，INACTIVE-非活跃，DISABLED-禁用
     */
    private Integer status;

    /**
     * 组织ID
     */
    private Long oid;

    /**
     * 首次登录时间 - 上云时间
     */
    private LocalDateTime firstLoginTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 逻辑删除标记
     */
    private Boolean deleted;
}
