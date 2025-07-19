# 前端"包含子组"功能设计方案

## 1. 功能概述

在终端组权限管理中，前端需要提供直观的界面让用户控制"包含子组"的绑定关系。用户可以为指定的用户组添加对某个终端组的权限，并选择是否包含该终端组的所有子组。

## 2. 核心需求

### 2.1 权限控制粒度
- 用户可以为终端组单独设置权限
- 用户可以选择是否包含子组权限
- 包含子组权限时，自动获得该终端组下所有子组的权限

### 2.2 交互要求
- 清晰的视觉反馈：用户能看到当前权限状态
- 简单的操作流程：一次操作完成权限设置
- 智能的默认值：新项目应该有合理的默认设置

## 3. 前端界面设计

### 3.1 权限管理主界面
```
用户组权限管理
├── 用户组选择器
└── 终端组权限树
    ├── 根终端组A
    │   ├── [✓] 权限开关
    │   ├── [✓] 包含子组
    │   └── 子组A1
    │       ├── [✓] 权限开关
    │       └── [✗] 包含子组
    └── 根终端组B
        ├── [✗] 权限开关
        └── [✗] 包含子组
```

### 3.2 权限状态显示
- **直接权限**: 显示绿色勾选标记
- **继承权限**: 显示灰色勾选标记（从父组继承）
- **无权限**: 显示未勾选状态

### 3.3 包含子组控制
- **独立开关**: 每个终端组都有独立的"包含子组"开关
- **智能提示**: 当用户勾选"包含子组"时，显示将要包含的子组列表
- **冲突检测**: 避免冗余的权限设置

## 4. 数据结构设计

### 4.1 请求数据结构
```typescript
interface BatchBindingOperationRequest {
  ugid: number;                    // 用户组ID
  grantPermissions: TerminalGroupPermission[];  // 要添加的权限
  revokeTerminalGroupIds: number[];             // 要移除的终端组ID
  description?: string;                         // 操作说明
}

interface TerminalGroupPermission {
  tgid: number;                    // 终端组ID
  includeChildren: boolean;        // 是否包含子组
  description?: string;            // 权限说明
}
```

### 4.2 响应数据结构
```typescript
interface BatchBindingOperationResponse {
  success: boolean;
  message: string;
  statistics: {
    grantedCount: number;          // 添加权限数量
    revokedCount: number;          // 移除权限数量
    noChangeCount: number;         // 无变化数量
  };
  details: OperationDetail[];      // 操作详情
}
```

## 5. 交互流程设计

### 5.1 权限授予流程
1. 用户选择要授权的终端组
2. 勾选"权限开关"
3. 选择是否"包含子组"
4. 点击"保存"提交请求
5. 系统返回操作结果

### 5.2 包含子组的智能提示
```
当用户勾选"包含子组"时：
┌─────────────────────────────────────┐
│ 包含子组提示                          │
├─────────────────────────────────────┤
│ 启用"包含子组"后，将自动获得以下权限：   │
│ • 子组A1                            │
│ • 子组A2                            │
│ • 子组A3                            │
│                                     │
│ [确认] [取消]                        │
└─────────────────────────────────────┘
```

### 5.3 批量操作支持
- 支持多选终端组进行批量权限设置
- 支持批量设置"包含子组"属性
- 提供批量操作的确认对话框

## 6. 前端实现要点

### 6.1 状态管理
```typescript
interface PermissionState {
  userGroupId: number;
  terminalGroupTree: TerminalGroupTreeNode[];
  selectedPermissions: Map<number, PermissionSetting>;
  loading: boolean;
  error: string | null;
}

interface PermissionSetting {
  hasPermission: boolean;
  includeChildren: boolean;
  permissionSource: 'direct' | 'inherited';
}
```

### 6.2 组件设计
- **PermissionTreeNode**: 单个终端组权限控制组件
- **PermissionManager**: 权限管理主组件
- **BatchOperationDialog**: 批量操作确认对话框
- **PermissionStatusIndicator**: 权限状态显示组件

### 6.3 关键逻辑
```typescript
// 权限变更处理
const handlePermissionChange = (tgid: number, hasPermission: boolean, includeChildren: boolean) => {
  const newPermissions = new Map(selectedPermissions);
  
  if (hasPermission) {
    newPermissions.set(tgid, {
      hasPermission: true,
      includeChildren: includeChildren,
      permissionSource: 'direct'
    });
  } else {
    newPermissions.delete(tgid);
  }
  
  setSelectedPermissions(newPermissions);
};

// 提交权限变更
const submitPermissionChanges = async () => {
  const grantPermissions = Array.from(selectedPermissions.entries())
    .filter(([_, setting]) => setting.hasPermission)
    .map(([tgid, setting]) => ({
      tgid,
      includeChildren: setting.includeChildren
    }));
  
  const revokeTerminalGroupIds = Array.from(originalPermissions.keys())
    .filter(tgid => !selectedPermissions.has(tgid));
  
  await updateUserGroupPermissions({
    ugid: userGroupId,
    grantPermissions,
    revokeTerminalGroupIds
  });
};
```

## 7. 默认值策略

### 7.1 新项目推荐策略
作为新项目，建议采用以下默认策略：

1. **默认不包含子组**: 新权限默认 `includeChildren = false`
2. **显式控制**: 用户需要明确选择是否包含子组
3. **智能提示**: 当选择包含子组时，显示影响范围

### 7.2 用户体验优化
- 提供"快速配置"选项，一键设置常用权限模式
- 支持权限模板，保存常用的权限配置
- 提供权限预览功能，显示最终权限效果

## 8. 技术实现细节

### 8.1 API调用
```typescript
// 更新用户组权限
const updateUserGroupPermissions = async (request: BatchBindingOperationRequest): Promise<BatchBindingOperationResponse> => {
  const response = await fetch('/api/terminal-groups/permissions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
  
  if (!response.ok) {
    throw new Error('Failed to update permissions');
  }
  
  return response.json();
};
```

### 8.2 权限计算
```typescript
// 计算最终权限（包含继承）
const calculateFinalPermissions = (directPermissions: PermissionSetting[], terminalGroupTree: TerminalGroupTreeNode[]): Map<number, PermissionSetting> => {
  const finalPermissions = new Map<number, PermissionSetting>();
  
  // 处理直接权限
  directPermissions.forEach(permission => {
    finalPermissions.set(permission.tgid, permission);
    
    // 如果包含子组，添加子组权限
    if (permission.includeChildren) {
      const childGroups = getChildGroups(permission.tgid, terminalGroupTree);
      childGroups.forEach(childGroup => {
        if (!finalPermissions.has(childGroup.tgid)) {
          finalPermissions.set(childGroup.tgid, {
            hasPermission: true,
            includeChildren: false,
            permissionSource: 'inherited'
          });
        }
      });
    }
  });
  
  return finalPermissions;
};
```

## 9. 总结

这个设计方案解决了用户在新项目中管理"包含子组"权限关系的问题：

1. **清晰的界面设计**: 用户可以直观地看到和控制权限
2. **灵活的权限控制**: 支持细粒度的权限设置
3. **智能的默认策略**: 新项目采用保守的默认设置
4. **完善的交互流程**: 从权限设置到提交的完整流程
5. **强大的技术支持**: 完整的前后端协作方案

通过这个方案，用户可以轻松地管理复杂的终端组权限关系，同时保持系统的灵活性和可扩展性。