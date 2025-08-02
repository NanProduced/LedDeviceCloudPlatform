# 素材上传流程优化设计方案

## 问题分析

### 原有流程问题
```
前端 → Core-Service → File-Service
```
**缺陷**：
1. **两次数据传输** - 文件先传到Core-Service，再转发到File-Service
2. **性能瓶颈** - Core-Service成为文件传输瓶颈
3. **资源浪费** - 重复的网络传输和临时存储
4. **用户体验差** - 大文件上传时间翻倍

## 优化方案：前端直传 + 权限预检查

### 核心设计原则
1. **权限前置检查** - 在Core-Service进行权限验证和配额检查
2. **授权令牌机制** - 生成临时上传令牌，允许前端直传File-Service
3. **事件驱动协调** - 通过消息队列协调两个服务的数据同步
4. **MD5秒传优化** - 在File-Service层面实现去重和秒传

## 详细上传流程

### 阶段1：上传预检查（Core-Service）

#### 1.1 前端发起上传预检查
```javascript
// 前端JavaScript
const uploadPreCheck = async (fileInfo) => {
    const response = await fetch('/api/v1/material/upload/precheck', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + accessToken
        },
        body: JSON.stringify({
            fileName: fileInfo.name,
            fileSize: fileInfo.size,
            fileMD5: await calculateMD5(fileInfo), // 前端计算MD5
            materialName: materialName,
            userGroupId: targetUserGroupId,
            folderId: targetFolderId,
            description: description,
            tags: tags
        })
    });
    
    return response.json();
};
```

#### 1.2 Core-Service预检查逻辑
```java
@PostMapping("/upload/precheck")
public Result<UploadPrecheckResponse> uploadPrecheck(@RequestBody UploadPrecheckRequest request) {
    GatewayUserInfo userInfo = LoginUserHolder.getCurrentUser();
    
    // 1. 权限检查
    permissionService.validateMaterialUploadPermission(
        userInfo.getUid(), userInfo.getUserGroupId(), userInfo.getOid(), 
        request.getUserGroupId(), request.getFolderId());
    
    // 2. 配额检查
    quotaService.validateFileUpload(userInfo.getOid(), request.getFileSize());
    
    // 3. MD5去重检查
    Optional<FileEntity> existingFile = fileServiceClient.checkFileExists(request.getFileMD5());
    if (existingFile.isPresent()) {
        // 秒传处理
        return handleInstantUpload(existingFile.get(), request, userInfo);
    }
    
    // 4. 生成上传令牌
    UploadToken uploadToken = generateUploadToken(request, userInfo);
    
    // 5. 返回授权信息
    return Result.success(UploadPrecheckResponse.builder()
        .canUpload(true)
        .uploadToken(uploadToken.getToken())
        .uploadUrl(fileServiceUploadUrl)
        .expiresAt(uploadToken.getExpiresAt())
        .chunkSize(CHUNK_SIZE) // 分片大小
        .build());
}
```

### 阶段2：前端直传File-Service

#### 2.1 大文件分片上传
```javascript
// 前端分片上传实现
const uploadLargeFile = async (file, uploadInfo) => {
    const chunkSize = uploadInfo.chunkSize;
    const totalChunks = Math.ceil(file.size / chunkSize);
    const uploadPromises = [];
    
    for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize;
        const end = Math.min(start + chunkSize, file.size);
        const chunk = file.slice(start, end);
        
        uploadPromises.push(uploadChunk(chunk, i, totalChunks, uploadInfo));
    }
    
    // 并行上传分片
    const results = await Promise.all(uploadPromises);
    
    // 合并分片
    return await mergeChunks(uploadInfo.uploadToken, totalChunks);
};

const uploadChunk = async (chunk, chunkIndex, totalChunks, uploadInfo) => {
    const formData = new FormData();
    formData.append('file', chunk);
    formData.append('uploadToken', uploadInfo.uploadToken);
    formData.append('chunkIndex', chunkIndex);
    formData.append('totalChunks', totalChunks);
    
    return fetch(uploadInfo.uploadUrl + '/chunk', {
        method: 'POST',
        body: formData
    });
};
```

#### 2.2 File-Service接收处理
```java
@PostMapping("/upload/chunk")
public Result<ChunkUploadResponse> uploadChunk(
        @RequestParam("file") MultipartFile chunk,
        @RequestParam("uploadToken") String uploadToken,
        @RequestParam("chunkIndex") int chunkIndex,
        @RequestParam("totalChunks") int totalChunks) {
    
    // 1. 验证上传令牌
    UploadTokenInfo tokenInfo = validateUploadToken(uploadToken);
    
    // 2. 保存分片
    String chunkPath = saveChunk(chunk, tokenInfo.getUploadId(), chunkIndex);
    
    // 3. 检查是否所有分片都已上传
    if (isAllChunksUploaded(tokenInfo.getUploadId(), totalChunks)) {
        // 触发分片合并
        return Result.success(ChunkUploadResponse.builder()
            .chunkIndex(chunkIndex)
            .uploaded(true)
            .canMerge(true)
            .build());
    }
    
    return Result.success(ChunkUploadResponse.builder()
        .chunkIndex(chunkIndex)
        .uploaded(true)
        .canMerge(false)
        .build());
}

@PostMapping("/upload/merge")
public Result<FileUploadResponse> mergeChunks(@RequestParam("uploadToken") String uploadToken) {
    try {
        // 1. 验证令牌
        UploadTokenInfo tokenInfo = validateUploadToken(uploadToken);
        
        // 2. 合并分片
        String finalFilePath = mergeChunks(tokenInfo.getUploadId());
        
        // 3. 计算MD5并检查去重
        String actualMD5 = calculateFileMD5(finalFilePath);
        if (!actualMD5.equals(tokenInfo.getExpectedMD5())) {
            throw new BusinessException("文件MD5校验失败");
        }
        
        // 4. 创建FileEntity
        FileEntity fileEntity = createFileEntity(tokenInfo, finalFilePath, actualMD5);
        fileEntityRepository.save(fileEntity);
        
        // 5. 发布文件上传完成事件
        publishFileUploadedEvent(fileEntity, tokenInfo);
        
        return Result.success(FileUploadResponse.builder()
            .fileEntityId(fileEntity.getFileEntityId())
            .fileMD5(actualMD5)
            .fileSize(fileEntity.getFileSize())
            .build());
            
    } catch (Exception e) {
        log.error("合并分片失败: uploadToken={}, error={}", uploadToken, e.getMessage(), e);
        throw new BusinessException("文件上传失败");
    }
}
```

### 阶段3：Core-Service元数据创建

#### 3.1 监听文件上传完成事件
```java
@RabbitListener(queues = "file.uploaded.queue")
public void handleFileUploadedEvent(FileUploadedEvent event) {
    try {
        log.info("处理文件上传完成事件: fileEntityId={}, uploadToken={}", 
                event.getFileEntityId(), event.getUploadToken());
        
        // 1. 验证上传令牌
        UploadTokenInfo tokenInfo = uploadTokenService.getTokenInfo(event.getUploadToken());
        if (tokenInfo == null || tokenInfo.isExpired()) {
            log.error("上传令牌无效或已过期: uploadToken={}", event.getUploadToken());
            return;
        }
        
        // 2. 创建素材元数据
        UserMaterial material = UserMaterial.builder()
                .materialId(generateMaterialId())
                .materialName(tokenInfo.getMaterialName())
                .fileEntityId(event.getFileEntityId())
                .orgId(tokenInfo.getOrgId())
                .userGroupId(tokenInfo.getUserGroupId())
                .folderId(tokenInfo.getFolderId())
                .materialType(determineMaterialType(tokenInfo.getFileName()))
                .description(tokenInfo.getDescription())
                .tags(tokenInfo.getTags())
                .usageCount(0)
                .uploadedBy(tokenInfo.getUploadedBy())
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        
        materialRepository.save(material);
        
        // 3. 更新配额使用量
        quotaService.addFileUsage(tokenInfo.getOrgId(), event.getFileSize());
        
        // 4. 更新存储统计
        statisticsService.updateStatisticsOnMaterialUpload(
                tokenInfo.getOrgId(), tokenInfo.getUserGroupId(), 
                material.getMaterialType(), event.getFileSize());
        
        // 5. 发布素材创建完成事件
        publishMaterialCreatedEvent(material);
        
        // 6. 清理上传令牌
        uploadTokenService.invalidateToken(event.getUploadToken());
        
        log.info("素材创建完成: materialId={}, fileEntityId={}", 
                material.getMaterialId(), event.getFileEntityId());
        
    } catch (Exception e) {
        log.error("处理文件上传完成事件失败: fileEntityId={}, error={}", 
                event.getFileEntityId(), e.getMessage(), e);
        // TODO: 可以考虑重试机制或死信队列处理
    }
}
```

## 核心组件实现

### 1. 上传令牌服务
```java
@Service
public class UploadTokenService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    
    public UploadToken generateUploadToken(UploadPrecheckRequest request, GatewayUserInfo userInfo) {
        String uploadId = generateUploadId();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2); // 2小时有效期
        
        UploadTokenInfo tokenInfo = UploadTokenInfo.builder()
                .uploadId(uploadId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .expectedMD5(request.getFileMD5())
                .materialName(request.getMaterialName())
                .orgId(userInfo.getOid())
                .userGroupId(request.getUserGroupId())
                .folderId(request.getFolderId())
                .description(request.getDescription())
                .tags(request.getTags())
                .uploadedBy(userInfo.getUid())
                .expiresAt(expiresAt)
                .build();
        
        // 生成JWT令牌
        String token = jwtTokenProvider.generateUploadToken(tokenInfo);
        
        // 存储到Redis（用于快速验证）
        String redisKey = "upload:token:" + token;
        redisTemplate.opsForValue().set(redisKey, tokenInfo, Duration.ofHours(2));
        
        return UploadToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }
    
    public UploadTokenInfo validateUploadToken(String token) {
        String redisKey = "upload:token:" + token;
        UploadTokenInfo tokenInfo = (UploadTokenInfo) redisTemplate.opsForValue().get(redisKey);
        
        if (tokenInfo == null) {
            // 从JWT解析（防止Redis失效）
            tokenInfo = jwtTokenProvider.parseUploadToken(token);
        }
        
        if (tokenInfo == null || tokenInfo.isExpired()) {
            throw new BusinessException(ExceptionEnum.INVALID_TOKEN, "上传令牌无效或已过期");
        }
        
        return tokenInfo;
    }
}
```

### 2. MD5秒传机制
```java
@Service
public class InstantUploadService {
    
    public Result<UploadPrecheckResponse> handleInstantUpload(FileEntity existingFile, 
                                                             UploadPrecheckRequest request, 
                                                             GatewayUserInfo userInfo) {
        try {
            // 1. 创建素材元数据（复用已存在的文件实体）
            UserMaterial material = UserMaterial.builder()
                    .materialId(generateMaterialId())
                    .materialName(request.getMaterialName())
                    .fileEntityId(existingFile.getFileEntityId())
                    .orgId(userInfo.getOid())
                    .userGroupId(request.getUserGroupId())
                    .folderId(request.getFolderId())
                    .materialType(determineMaterialType(request.getFileName()))
                    .description(request.getDescription())
                    .tags(request.getTags())
                    .usageCount(0)
                    .uploadedBy(userInfo.getUid())
                    .createdTime(LocalDateTime.now())
                    .updatedTime(LocalDateTime.now())
                    .build();
            
            materialRepository.save(material);
            
            // 2. 更新配额使用量（文件已存在，只增加引用计数）
            quotaService.addFileUsage(userInfo.getOid(), 0L); // 只增加文件数量，不增加存储大小
            
            // 3. 更新统计
            statisticsService.updateStatisticsOnMaterialUpload(
                    userInfo.getOid(), request.getUserGroupId(), 
                    material.getMaterialType(), 0L);
            
            // 4. 发布素材创建完成事件
            publishMaterialCreatedEvent(material);
            
            log.info("MD5秒传完成: materialId={}, fileEntityId={}, md5={}", 
                    material.getMaterialId(), existingFile.getFileEntityId(), existingFile.getFileMD5());
            
            return Result.success(UploadPrecheckResponse.builder()
                    .canUpload(false)
                    .isInstantUpload(true)
                    .materialId(material.getMaterialId())
                    .fileEntityId(existingFile.getFileEntityId())
                    .message("文件已存在，秒传成功")
                    .build());
                    
        } catch (Exception e) {
            log.error("MD5秒传失败: md5={}, error={}", existingFile.getFileMD5(), e.getMessage(), e);
            throw new BusinessException(ExceptionEnum.SYSTEM_ERROR, "秒传处理失败");
        }
    }
}
```

## 性能优化特性

### 1. 断点续传支持
- **分片状态跟踪** - Redis记录每个分片的上传状态
- **续传检测** - 前端检查已上传分片，从断点继续
- **清理机制** - 过期未完成的分片自动清理

### 2. 并发上传优化
- **分片并行上传** - 多个分片同时上传，提高效率
- **动态分片大小** - 根据网络状况调整分片大小
- **重试机制** - 失败分片自动重试

### 3. 网络优化
- **CDN加速** - File-Service部署到CDN节点
- **压缩传输** - 支持gzip压缩
- **连接复用** - HTTP/2多路复用

## 错误处理与回滚

### 1. 上传失败处理
```java
@EventListener
public void handleUploadFailure(UploadFailureEvent event) {
    try {
        // 1. 清理临时文件
        fileStorageService.cleanupTempFiles(event.getUploadId());
        
        // 2. 回滚配额占用
        if (event.getTokenInfo() != null) {
            quotaService.removeFileUsage(event.getTokenInfo().getOrgId(), 
                                       event.getTokenInfo().getFileSize());
        }
        
        // 3. 通知前端失败
        notificationService.notifyUploadFailure(event.getUserId(), event.getError());
        
        // 4. 清理上传令牌
        uploadTokenService.invalidateToken(event.getUploadToken());
        
    } catch (Exception e) {
        log.error("处理上传失败回滚异常: uploadId={}, error={}", 
                event.getUploadId(), e.getMessage(), e);
    }
}
```

### 2. 网络中断恢复
- **状态持久化** - 上传状态存储到本地存储
- **自动恢复** - 页面刷新后自动恢复上传
- **进度展示** - 实时显示上传进度和已完成分片

## 监控与告警

### 1. 上传性能监控
- **上传速度统计** - 实时监控上传带宽使用
- **成功率统计** - 上传成功率和失败原因分析
- **用户体验指标** - 平均上传时间、重试次数等

### 2. 系统资源监控
- **File-Service负载** - CPU、内存、磁盘IO监控
- **Redis使用情况** - 令牌缓存、分片状态缓存监控
- **消息队列积压** - 事件处理延迟监控

## 总结

通过**前端直传 + 权限预检查**的架构设计，我们成功解决了原有流程的性能问题：

1. **消除重复传输** - 文件只传输一次，直达File-Service
2. **提升用户体验** - 大文件上传时间减半，支持断点续传
3. **增强系统性能** - Core-Service不再是文件传输瓶颈
4. **保持权限控制** - 通过令牌机制确保安全性不降低
5. **支持高级特性** - MD5秒传、分片并行上传、错误恢复

这个架构既保持了系统的安全性和权限控制，又大幅提升了文件上传的性能和用户体验。