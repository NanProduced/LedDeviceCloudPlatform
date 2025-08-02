package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Folder {

    private Long fid;

    private String folderName;

    private Long oid;

    private Long ugid;

    private String description;

    /**
     * 1.子文件夹的父文件夹ID
     * 2.如果是PUBLIC，则为null
     * 3.用户组下根文件夹，为null
     */
    private Long parent;

    private String path;

    /**
     * NORMAL: 普通文件夹
     * PUBLIC: 公共资源组
     */
    private String folderType;

    /**
     * 是否被分享
     */
    public boolean shared;

    private Long creatorId;

    private LocalDateTime createTime;

    private Long updaterId;

    private LocalDateTime updateTime;
}
