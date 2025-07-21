package org.nan.cloud.message.infrastructure.mongodb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.nan.cloud.message.infrastructure.mongodb.document.TemplateContent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息模板内容Repository接口
 * 
 * 提供消息模板内容的MongoDB数据访问功能，支持模板管理和内容检索。
 * 配合MySQL的模板元数据，提供完整的模板管理能力。
 * 
 * 核心功能：
 * - 模板内容的CRUD操作
 * - 按组织和类型查询模板
 * - 模板变量和样式管理
 * - 模板版本控制
 * - 多语言模板支持
 * 
 * @author Nan
 * @since 1.0.0
 */
@Repository
public interface TemplateContentRepository extends MongoRepository<TemplateContent, String> {
    
    /**
     * 根据模板ID查询模板内容
     * 
     * @param templateId 模板ID
     * @return 模板内容（可选）
     */
    Optional<TemplateContent> findByTemplateId(String templateId);
    
    /**
     * 查询组织的启用模板
     * 
     * 按模板类型查询组织内启用的模板，用于模板选择界面。
     * 
     * @param organizationId 组织ID
     * @param templateType 模板类型
     * @param isActive 是否启用
     * @return 启用的模板列表
     */
    List<TemplateContent> findByOrganizationIdAndTemplateTypeAndIsActiveOrderByUpdatedTimeDesc(
            String organizationId, String templateType, Boolean isActive);
    
    /**
     * 查询组织的所有模板
     * 
     * 支持模板管理界面的完整模板列表显示。
     * 
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 分页模板内容
     */
    Page<TemplateContent> findByOrganizationIdOrderByUpdatedTimeDesc(
            String organizationId, Pageable pageable);
    
    /**
     * 根据模板类型查询模板
     * 
     * 按模板类型分类查询，支持不同类型模板的独立管理。
     * 
     * @param organizationId 组织ID
     * @param templateType 模板类型
     * @param pageable 分页参数
     * @return 分页模板内容
     */
    Page<TemplateContent> findByOrganizationIdAndTemplateTypeOrderByUpdatedTimeDesc(
            String organizationId, String templateType, Pageable pageable);
    
    /**
     * 全文搜索模板内容
     * 
     * 在模板名称、标题模板和内容模板中搜索关键词。
     * 支持模板的快速查找和定位。
     * 
     * @param organizationId 组织ID（数据隔离）
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的模板列表
     */
    @Query("{ 'organizationId': ?0, $text: { $search: ?1 } }")
    Page<TemplateContent> searchTemplatesByKeyword(String organizationId, String keyword, Pageable pageable);
    
    /**
     * 查询包含特定变量的模板
     * 
     * 根据模板变量查找相关模板，用于变量管理和模板分析。
     * 
     * @param organizationId 组织ID
     * @param variableName 变量名称
     * @return 包含指定变量的模板列表
     */
    @Query("{ 'organizationId': ?0, 'variables.name': ?1 }")
    List<TemplateContent> findTemplatesWithVariable(String organizationId, String variableName);
    
    /**
     * 查询支持多语言的模板
     * 
     * 查询配置了多语言的模板，用于国际化功能管理。
     * 
     * @param organizationId 组织ID
     * @param language 语言代码
     * @return 支持指定语言的模板列表
     */
    @Query("{ 'organizationId': ?0, 'localization.?1': { $exists: true } }")
    List<TemplateContent> findTemplatesWithLocalization(String organizationId, String language);
    
    /**
     * 查询最近更新的模板
     * 
     * 用于展示最近修改的模板，便于管理员跟踪模板变更。
     * 
     * @param organizationId 组织ID
     * @param afterTime 时间点
     * @param pageable 分页参数
     * @return 最近更新的模板列表
     */
    Page<TemplateContent> findByOrganizationIdAndUpdatedTimeAfterOrderByUpdatedTimeDesc(
            String organizationId, LocalDateTime afterTime, Pageable pageable);
    
    /**
     * 统计组织模板数量
     * 
     * 按模板类型统计组织内的模板数量。
     * 
     * @param organizationId 组织ID
     * @param templateType 模板类型
     * @param isActive 是否启用
     * @return 模板数量
     */
    long countByOrganizationIdAndTemplateTypeAndIsActive(
            String organizationId, String templateType, Boolean isActive);
    
    /**
     * 查询模板版本历史
     * 
     * 根据模板基础ID查询所有版本，支持版本管理。
     * 模板ID格式：base-id-v1.0.0，通过正则匹配查找同一模板的所有版本。
     * 
     * @param templateIdPrefix 模板ID前缀
     * @param organizationId 组织ID
     * @return 版本历史列表
     */
    @Query("{ 'organizationId': ?1, 'templateId': { $regex: '^?0', $options: 'i' } }")
    List<TemplateContent> findTemplateVersionHistory(String templateIdPrefix, String organizationId);
    
    /**
     * 查询包含富文本内容的模板
     * 
     * 查询配置了HTML富文本的模板，用于富文本功能管理。
     * 
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 包含富文本的模板列表
     */
    @Query("{ 'organizationId': ?0, 'richContentTemplate': { $exists: true, $ne: null } }")
    Page<TemplateContent> findTemplatesWithRichContent(String organizationId, Pageable pageable);
    
    /**
     * 查询包含样式配置的模板
     * 
     * 查询配置了自定义样式的模板，用于样式管理。
     * 
     * @param organizationId 组织ID
     * @return 包含样式配置的模板列表
     */
    @Query("{ 'organizationId': ?0, 'styleConfig': { $exists: true, $ne: null } }")
    List<TemplateContent> findTemplatesWithStyles(String organizationId);
    
    /**
     * 查询包含预设值的模板
     * 
     * 查询配置了变量预设值的模板，便于预设值管理。
     * 
     * @param organizationId 组织ID
     * @return 包含预设值的模板列表
     */
    @Query("{ 'organizationId': ?0, 'presetValues': { $exists: true, $not: { $size: 0 } } }")
    List<TemplateContent> findTemplatesWithPresetValues(String organizationId);
    
    /**
     * 根据变量类型查询模板
     * 
     * 查询包含特定类型变量的模板，用于变量类型统计和管理。
     * 
     * @param organizationId 组织ID
     * @param variableType 变量类型
     * @return 包含指定类型变量的模板列表
     */
    @Query("{ 'organizationId': ?0, 'variables.type': ?1 }")
    List<TemplateContent> findTemplatesByVariableType(String organizationId, String variableType);
    
    /**
     * 查询包含验证规则的模板
     * 
     * 查询配置了验证规则的模板，用于模板质量管理。
     * 
     * @param organizationId 组织ID
     * @return 包含验证规则的模板列表
     */
    @Query("{ 'organizationId': ?0, 'validationRules': { $exists: true, $ne: null } }")
    List<TemplateContent> findTemplatesWithValidation(String organizationId);
    
    /**
     * 查询模板使用示例
     * 
     * 查询包含使用示例的模板，便于用户学习和参考。
     * 
     * @param organizationId 组织ID
     * @param templateType 模板类型
     * @return 包含示例的模板列表
     */
    @Query("{ 'organizationId': ?0, 'templateType': ?1, 'examples': { $exists: true, $not: { $size: 0 } } }")
    List<TemplateContent> findTemplatesWithExamples(String organizationId, String templateType);
    
    /**
     * 复杂查询：按多个条件筛选模板
     * 
     * 支持组合条件查询，用于高级模板搜索功能。
     * 
     * @param organizationId 组织ID
     * @param templateType 模板类型（可选）
     * @param isActive 是否启用（可选）
     * @param hasRichContent 是否包含富文本（可选）
     * @param pageable 分页参数
     * @return 匹配条件的模板列表
     */
    @Query("{ " +
           "'organizationId': ?0, " +
           "$and: [" +
           "{ $or: [ { $expr: { $eq: [?1, null] } }, { 'templateType': ?1 } ] }, " +
           "{ $or: [ { $expr: { $eq: [?2, null] } }, { 'isActive': ?2 } ] }, " +
           "{ $or: [ { $expr: { $eq: [?3, null] } }, " +
           "{ $expr: { $cond: [ ?3, { $ne: ['$richContentTemplate', null] }, { $eq: ['$richContentTemplate', null] } ] } } ] }" +
           "]" +
           "}")
    Page<TemplateContent> findTemplatesByMultipleConditions(
            String organizationId, String templateType, Boolean isActive, 
            Boolean hasRichContent, Pageable pageable);
    
    /**
     * 删除过期的模板版本
     * 
     * 删除创建时间较早的模板版本，进行版本清理。
     * 
     * @param organizationId 组织ID
     * @param beforeTime 过期时间点
     * @return 删除的模板数量
     */
    long deleteByOrganizationIdAndCreatedTimeBefore(String organizationId, LocalDateTime beforeTime);
}