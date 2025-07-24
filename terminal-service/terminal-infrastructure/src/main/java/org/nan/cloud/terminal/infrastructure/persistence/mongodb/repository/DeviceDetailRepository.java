package org.nan.cloud.terminal.infrastructure.persistence.mongodb.repository;

import org.nan.cloud.terminal.infrastructure.persistence.mongodb.document.DeviceDetailDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备详情MongoDB数据访问层
 * 
 * 基于Spring Data MongoDB的文档存储访问接口，提供：
 * 1. 文档CRUD操作 - 继承MongoRepository获得标准操作方法
 * 2. 复杂配置查询 - 基于JSON字段的复杂查询和聚合
 * 3. 性能指标存储 - 时序数据的高效写入和查询
 * 4. 设备状态历史 - 状态变更记录的追加和分页查询
 * 5. 配置管理优化 - 部分字段更新和版本控制
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Repository
public interface DeviceDetailRepository extends MongoRepository<DeviceDetailDocument, String> {

    /**
     * 根据设备ID查询设备详情
     * 
     * @param deviceId 设备ID
     * @return 设备详情文档
     */
    Optional<DeviceDetailDocument> findByDeviceId(String deviceId);

    /**
     * 根据组织ID查询设备详情列表
     * 支持分页查询，用于组织级别的设备管理
     * 
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 分页设备详情列表
     */
    Page<DeviceDetailDocument> findByOrganizationId(String organizationId, Pageable pageable);

    /**
     * 根据设备ID和组织ID查询设备详情
     * 多租户场景下的安全查询
     * 
     * @param deviceId 设备ID
     * @param organizationId 组织ID
     * @return 设备详情文档
     */
    Optional<DeviceDetailDocument> findByDeviceIdAndOrganizationId(String deviceId, String organizationId);

    /**
     * 查询指定时间范围内更新的设备详情
     * 用于增量同步和变更监控
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 更新的设备详情列表
     */
    @Query("{'updatedAt': {$gte: ?0, $lte: ?1}}")
    List<DeviceDetailDocument> findByUpdatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据LED配置参数查询设备
     * 支持复杂的嵌套字段查询
     * 
     * @param resolution 分辨率范围查询
     * @param organizationId 组织ID
     * @return 匹配的设备列表
     */
    @Query("{'organizationId': ?1, 'ledConfig.resolution.width': {$gte: ?0.width}, " +
           "'ledConfig.resolution.height': {$gte: ?0.height}}")
    List<DeviceDetailDocument> findByLedConfigResolution(
        DeviceDetailDocument.Resolution resolution, String organizationId);

    /**
     * 查询支持特定媒体格式的设备
     * 基于播放配置中的支持格式列表
     * 
     * @param format 媒体格式
     * @param organizationId 组织ID
     * @return 支持该格式的设备列表
     */
    @Query("{'organizationId': ?1, 'playbackConfig.supportedFormats': {$in: [?0]}}")
    List<DeviceDetailDocument> findBySupportedFormat(String format, String organizationId);

    /**
     * 查询具有特定硬件能力的设备
     * 基于设备能力描述进行查询
     * 
     * @param cpuCores CPU核心数最小值
     * @param memorySize 内存大小最小值
     * @param organizationId 组织ID
     * @return 满足硬件要求的设备列表
     */
    @Query("{'organizationId': ?2, " +
           "'deviceCapabilities.cpuInfo.cores': {$gte: ?0}, " +
           "'deviceCapabilities.memoryInfo.totalSize': {$gte: ?1}}")
    List<DeviceDetailDocument> findByHardwareCapabilities(
        Integer cpuCores, Long memorySize, String organizationId);

    /**
     * 更新设备LED配置
     * 部分字段更新，提升性能
     * 
     * @param deviceId 设备ID
     * @param ledConfig LED配置对象
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$set': {'ledConfig': ?1, 'updatedAt': ?#{new java.time.LocalDateTime()}, '$inc': {'version': 1}}}")
    void updateLedConfig(String deviceId, DeviceDetailDocument.LedConfig ledConfig);

    /**
     * 更新设备播放配置
     * 
     * @param deviceId 设备ID
     * @param playbackConfig 播放配置对象
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$set': {'playbackConfig': ?1, 'updatedAt': ?#{new java.time.LocalDateTime()}, '$inc': {'version': 1}}}")
    void updatePlaybackConfig(String deviceId, DeviceDetailDocument.PlaybackConfig playbackConfig);

    /**
     * 更新设备网络配置
     * 
     * @param deviceId 设备ID
     * @param networkConfig 网络配置对象
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$set': {'networkConfig': ?1, 'updatedAt': ?#{new java.time.LocalDateTime()}, '$inc': {'version': 1}}}")
    void updateNetworkConfig(String deviceId, DeviceDetailDocument.NetworkConfig networkConfig);

    /**
     * 添加设备状态历史记录
     * 使用$push操作符追加到状态历史数组
     * 
     * @param deviceId 设备ID
     * @param statusEntry 状态历史条目
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$push': {'statusHistory': {'$each': [?1], '$position': 0, '$slice': 100}}, " +
            "'$set': {'updatedAt': ?#{new java.time.LocalDateTime()}}, '$inc': {'version': 1}}")
    void addStatusHistory(String deviceId, DeviceDetailDocument.StatusHistoryEntry statusEntry);

    /**
     * 更新设备性能指标
     * 追加新的性能数据点
     * 
     * @param deviceId 设备ID
     * @param cpuMetric CPU使用率数据点
     * @param memoryMetric 内存使用率数据点
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$push': {" +
            "'performanceMetrics.cpuUsage': {'$each': [?1], '$slice': -1000}, " +
            "'performanceMetrics.memoryUsage': {'$each': [?2], '$slice': -1000}" +
            "}, '$set': {'updatedAt': ?#{new java.time.LocalDateTime()}}, '$inc': {'version': 1}}")
    void addPerformanceMetrics(String deviceId, 
                             DeviceDetailDocument.MetricPoint cpuMetric,
                             DeviceDetailDocument.MetricPoint memoryMetric);

    /**
     * 查询具有特定自定义属性的设备
     * 基于自定义扩展属性进行灵活查询
     * 
     * @param attributeKey 属性键
     * @param attributeValue 属性值
     * @param organizationId 组织ID
     * @return 匹配的设备列表
     */
    @Query("{'organizationId': ?2, ?#{'customAttributes.' + [0]}: ?1}")
    List<DeviceDetailDocument> findByCustomAttribute(
        String attributeKey, Object attributeValue, String organizationId);

    /**
     * 批量删除指定组织的设备详情
     * 用于组织注销时的数据清理
     * 
     * @param organizationId 组织ID
     * @return 删除的文档数量
     */
    Long deleteByOrganizationId(String organizationId);

    /**
     * 统计组织设备详情数量
     * 
     * @param organizationId 组织ID
     * @return 设备详情数量
     */
    Long countByOrganizationId(String organizationId);

    /**
     * 查询最近活跃的设备详情
     * 基于更新时间排序，用于活跃设备监控
     * 
     * @param organizationId 组织ID
     * @param limit 查询数量限制
     * @return 最近活跃的设备列表
     */
    @Query(value = "{'organizationId': ?0}", sort = "{'updatedAt': -1}")
    List<DeviceDetailDocument> findRecentActiveDevices(String organizationId, Pageable pageable);

    /**
     * 聚合查询设备配置统计
     * 统计不同配置类型的设备分布
     * 
     * @param organizationId 组织ID
     * @return 配置统计结果
     */
    @Query(value = "{'organizationId': ?0}")
    List<DeviceDetailDocument> findDeviceConfigStatistics(String organizationId);

    /**
     * 查询有性能告警的设备
     * 基于性能指标中的错误统计
     * 
     * @param organizationId 组织ID
     * @param errorThreshold 错误阈值
     * @return 有性能问题的设备列表
     */
    @Query("{'organizationId': ?0, 'performanceMetrics.errorStatistics.totalErrors': {$gte: ?1}}")
    List<DeviceDetailDocument> findDevicesWithPerformanceIssues(
        String organizationId, Integer errorThreshold);

    /**
     * 更新设备的自定义属性
     * 动态更新扩展属性字段
     * 
     * @param deviceId 设备ID
     * @param attributeKey 属性键
     * @param attributeValue 属性值
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$set': {?#{'customAttributes.' + [1]}: ?2, 'updatedAt': ?#{new java.time.LocalDateTime()}}, '$inc': {'version': 1}}")
    void updateCustomAttribute(String deviceId, String attributeKey, Object attributeValue);

    /**
     * 清理旧的性能监控数据
     * 删除指定时间之前的性能数据，节省存储空间
     * 
     * @param deviceId 设备ID
     * @param cutoffTime 截止时间
     */
    @Query("{'deviceId': ?0}")
    @Update("{'$pull': {" +
            "'performanceMetrics.cpuUsage': {'timestamp': {$lt: ?1}}, " +
            "'performanceMetrics.memoryUsage': {'timestamp': {$lt: ?1}}, " +
            "'performanceMetrics.networkTraffic': {'timestamp': {$lt: ?1}}" +
            "}, '$set': {'updatedAt': ?#{new java.time.LocalDateTime()}}, '$inc': {'version': 1}}")  
    void cleanupOldPerformanceData(String deviceId, LocalDateTime cutoffTime);

    /**
     * 根据多个条件组合查询设备
     * 支持复杂的多条件组合查询
     * 
     * @param organizationId 组织ID
     * @param minCpuCores 最小CPU核心数
     * @param minMemorySize 最小内存大小
     * @param supportedFormat 支持的媒体格式
     * @param pageable 分页参数
     * @return 符合条件的设备分页列表
     */
    @Query("{'organizationId': ?0, " +
           "'deviceCapabilities.cpuInfo.cores': {$gte: ?1}, " +
           "'deviceCapabilities.memoryInfo.totalSize': {$gte: ?2}, " +
           "'playbackConfig.supportedFormats': {$in: [?3]}}")
    Page<DeviceDetailDocument> findByMultipleConditions(
        String organizationId, Integer minCpuCores, Long minMemorySize, 
        String supportedFormat, Pageable pageable);
}