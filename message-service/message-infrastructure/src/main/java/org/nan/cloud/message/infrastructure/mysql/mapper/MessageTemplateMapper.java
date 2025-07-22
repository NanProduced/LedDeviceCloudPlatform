package org.nan.cloud.message.infrastructure.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.nan.cloud.message.infrastructure.mysql.entity.MessageTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息模板Mapper接口
 * 
 * 提供消息模板的数据库访问功能，支持模板管理和使用统计。
 * 实现模板的版本控制、启用状态管理和使用频率跟踪。
 * 
 * 核心功能：
 * - 模板的CRUD操作
 * - 模板启用状态管理
 * - 模板使用统计更新
 * - 按组织和类型查询
 * - 模板版本管理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplate> {
    
    /**
     * 根据模板ID查询启用的模板
     * 
     * @param templateId 模板ID
     * @param organizationId 组织ID（安全检查）
     * @return 模板记录
     */
    @Select("SELECT * FROM message_template " +
            "WHERE template_id = #{templateId} " +
            "AND organization_id = #{organizationId} " +
            "AND is_active = true " +
            "AND status = 'ACTIVE'")
    MessageTemplate selectActiveTemplate(@Param("templateId") String templateId,
                                        @Param("organizationId") String organizationId);
    
    /**
     * 查询组织的启用模板
     * 
     * 按模板类型和使用频率排序，用于模板选择界面。
     * 
     * @param organizationId 组织ID
     * @param templateType 模板类型（可选）
     * @param templateCategory 模板类别（可选）
     * @return 启用模板列表
     */
    @Select("<script>" +
            "SELECT * FROM message_template " +
            "WHERE organization_id = #{organizationId} " +
            "AND is_active = true " +
            "AND status = 'ACTIVE' " +
            "<if test='templateType != null and templateType != \"\"'>" +
            "AND template_type = #{templateType} " +
            "</if>" +
            "<if test='templateCategory != null and templateCategory != \"\"'>" +
            "AND template_category = #{templateCategory} " +
            "</if>" +
            "ORDER BY usage_count DESC, created_time DESC" +
            "</script>")
    List<MessageTemplate> selectActiveTemplates(@Param("organizationId") String organizationId,
                                               @Param("templateType") String templateType,
                                               @Param("templateCategory") String templateCategory);
    
    /**
     * 分页查询组织模板
     * 
     * 支持管理界面的模板列表显示，包含所有状态的模板。
     * 
     * @param page 分页参数
     * @param organizationId 组织ID
     * @param templateType 模板类型（可选）
     * @param status 模板状态（可选）
     * @param keyword 关键词搜索（可选）
     * @return 分页模板记录
     */
    @Select("<script>" +
            "SELECT * FROM message_template " +
            "WHERE organization_id = #{organizationId} " +
            "<if test='templateType != null and templateType != \"\"'>" +
            "AND template_type = #{templateType} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (template_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR content_summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY is_system DESC, usage_count DESC, updated_time DESC" +
            "</script>")
    IPage<MessageTemplate> selectTemplatesByPage(Page<MessageTemplate> page,
                                                 @Param("organizationId") String organizationId,
                                                 @Param("templateType") String templateType,
                                                 @Param("status") String status,
                                                 @Param("keyword") String keyword);
    
    /**
     * 更新模板使用统计
     * 
     * 每次使用模板时调用，更新使用次数和最后使用时间。
     * 
     * @param templateId 模板ID
     * @return 更新的记录数
     */
    @Update("UPDATE message_template SET " +
            "usage_count = usage_count + 1, " +
            "last_used_time = NOW(), " +
            "updated_time = NOW() " +
            "WHERE template_id = #{templateId}")
    int incrementUsageCount(@Param("templateId") String templateId);
    
    /**
     * 批量更新模板状态
     * 
     * 支持批量启用、停用或删除模板。
     * 
     * @param templateIds 模板ID列表
     * @param status 新状态
     * @param isActive 是否启用
     * @param updatedBy 更新者ID
     * @return 更新的记录数
     */
    @Update("<script>" +
            "UPDATE message_template SET " +
            "status = #{status}, " +
            "is_active = #{isActive}, " +
            "updated_by = #{updatedBy}, " +
            "updated_time = NOW() " +
            "WHERE template_id IN " +
            "<foreach collection='templateIds' item='templateId' open='(' separator=',' close=')'>" +
            "#{templateId}" +
            "</foreach>" +
            "AND is_system = false" + // 系统模板不允许批量修改
            "</script>")
    int batchUpdateStatus(@Param("templateIds") List<String> templateIds,
                         @Param("status") String status,
                         @Param("isActive") Boolean isActive,
                         @Param("updatedBy") String updatedBy);
    
    /**
     * 查询系统默认模板
     * 
     * 用于系统初始化时创建默认模板。
     * 
     * @param templateType 模板类型
     * @return 系统模板列表
     */
    @Select("SELECT * FROM message_template " +
            "WHERE is_system = true " +
            "AND template_type = #{templateType} " +
            "AND status = 'ACTIVE' " +
            "ORDER BY version DESC")
    List<MessageTemplate> selectSystemTemplates(@Param("templateType") String templateType);
    
    /**
     * 检查模板名称重复
     * 
     * 同一组织内模板名称不能重复。
     * 
     * @param templateName 模板名称
     * @param organizationId 组织ID
     * @param excludeId 排除的模板ID（编辑时使用）
     * @return 重复的模板数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM message_template " +
            "WHERE template_name = #{templateName} " +
            "AND organization_id = #{organizationId} " +
            "<if test='excludeId != null and excludeId != \"\"'>" +
            "AND template_id != #{excludeId} " +
            "</if>" +
            "</script>")
    Long countByName(@Param("templateName") String templateName,
                    @Param("organizationId") String organizationId,
                    @Param("excludeId") String excludeId);
    
    /**
     * 查询模板版本历史
     * 
     * 根据模板基础ID查询所有版本，用于版本管理。
     * 
     * @param baseTemplateId 基础模板ID（去除版本号部分）
     * @param organizationId 组织ID
     * @return 版本历史列表
     */
    @Select("SELECT * FROM message_template " +
            "WHERE template_id LIKE CONCAT(#{baseTemplateId}, '%') " +
            "AND organization_id = #{organizationId} " +
            "ORDER BY version DESC")
    List<MessageTemplate> selectVersionHistory(@Param("baseTemplateId") String baseTemplateId,
                                              @Param("organizationId") String organizationId);
    
    /**
     * 获取模板使用统计
     * 
     * 用于生成模板使用情况报表。
     * 
     * @param organizationId 组织ID
     * @param startTime 统计开始时间
     * @param endTime 统计结束时间
     * @return 统计结果列表
     */
    @Select("SELECT " +
            "template_type, " +
            "COUNT(*) as template_count, " +
            "SUM(usage_count) as total_usage, " +
            "AVG(usage_count) as avg_usage " +
            "FROM message_template " +
            "WHERE organization_id = #{organizationId} " +
            "AND created_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY template_type " +
            "ORDER BY total_usage DESC")
    List<java.util.Map<String, Object>> getTemplateUsageStatistics(@Param("organizationId") String organizationId,
                                                                   @Param("startTime") LocalDateTime startTime,
                                                                   @Param("endTime") LocalDateTime endTime);
}