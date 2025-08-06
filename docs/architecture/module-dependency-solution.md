# 模块依赖冲突解决方案

## 问题描述

将ProgramContent的VSN相关结构类移至`core-infrastructure`模块后，出现了模块依赖冲突：
- `core-api` 模块中的 `CreateProgramRequest` 依赖 `core-infrastructure` 模块的VSN类
- 违反了分层架构原则：API层不应该依赖基础设施层

## 问题根源

```
错误的依赖关系：
core-api ──→ core-infrastructure (❌)

正确的依赖关系：
core-boot ──→ core-application ──→ core-domain
                    ↑
core-infrastructure ──┘
```

## 解决方案

### 1. 创建API层专用DTO类

在 `core-api` 模块中创建纯净的DTO类，不依赖任何基础设施层：

- `ProgramInformationDTO` - 节目基础信息DTO
- `ProgramPageDTO` - 节目页面DTO
- `ProgramRegionDTO` - 节目区域DTO
- `ApiDtoValueObjects` - 其他值对象DTO集合

### 2. 创建转换器

在 `core-application` 模块中创建 `ProgramDtoConverter`：
- 负责API层DTO和infrastructure层实体之间的双向转换
- 保持分层架构清晰，避免层级依赖混乱

### 3. 更新服务实现

在 `ProgramServiceImpl` 中：
- 注入 `ProgramDtoConverter`
- 使用转换器处理DTO到实体的转换
- 保持业务逻辑与数据传输对象解耦

## 架构优势

### 1. 清晰的分层架构
```
┌─────────────────┐
│   core-api      │ ← 纯净的DTO，无技术依赖
└─────────────────┘
         ↓
┌─────────────────┐
│ core-application│ ← 转换器，业务逻辑
└─────────────────┘
         ↓
┌─────────────────┐
│  core-domain    │ ← 领域模型
└─────────────────┘
         ↑
┌─────────────────┐
│core-infrastructure│ ← VSN实体，MongoDB依赖
└─────────────────┘
```

### 2. 技术依赖隔离
- API层不依赖MongoDB、Spring Data等技术栈
- 基础设施层变更不影响API接口定义
- 便于不同层级的独立测试和部署

### 3. 数据转换灵活性
- 转换器可以处理复杂的数据映射逻辑
- 支持DTO和实体之间的字段差异
- 便于版本兼容性处理

## 文件变更清单

### 新增文件：
- `core-api/src/main/java/org/nan/cloud/core/api/DTO/program/ProgramInformationDTO.java`
- `core-api/src/main/java/org/nan/cloud/core/api/DTO/program/ProgramPageDTO.java`
- `core-api/src/main/java/org/nan/cloud/core/api/DTO/program/ProgramRegionDTO.java`
- `core-api/src/main/java/org/nan/cloud/core/api/DTO/program/ApiDtoValueObjects.java`
- `core-application/src/main/java/org/nan/cloud/core/converter/ProgramDtoConverter.java`

### 修改文件：
- `CreateProgramRequest.java` - 使用新的DTO类
- `ProgramServiceImpl.java` - 集成转换器
- `ProgramController.java` - 更新import
- `ProgramService.java` - 更新import

## 后续优化建议

1. **完善转换器**：实现所有值对象的转换方法
2. **单元测试**：为转换器添加完整的单元测试
3. **性能优化**：考虑使用MapStruct等工具自动生成转换代码
4. **文档完善**：为DTO类添加详细的API文档注释

## 总结

通过创建API层专用DTO和转换器，成功解决了模块依赖冲突问题，保持了清晰的分层架构，为后续开发奠定了良好的基础。