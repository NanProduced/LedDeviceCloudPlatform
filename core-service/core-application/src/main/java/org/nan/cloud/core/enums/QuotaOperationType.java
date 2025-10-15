package org.nan.cloud.core.enums;

public enum QuotaOperationType {
    // 素材新增，占用组织配额（+bytes, +1）
    MATERIAL_UPLOAD,

    // 素材删除，归还组织配额（-bytes, -1）
    MATERIAL_DELETE,

    // 导出文件生成（如需要计量，可作为组织层面的临时文件占用）
    EXPORT_FILE_GENERATE,

    // 导出文件删除（释放导出占用）
    EXPORT_FILE_DELETE,

    // 节目/VSN相关文件占用（预留）
    VSN_UPLOAD,

    // 节目/VSN相关文件删除（预留）
    VSN_DELETE,

    // 管理员手动调整（预留）
    QUOTA_ADJUST,

    // 其他未分类操作（预留）
    OTHER
}
