# 简化的前端交互设计

## 设计原则

1. **隐藏技术细节** - 用户不需要知道"直接绑定"和"继承权限"的区别
2. **简化用户概念** - 用户只需要知道"有权限"和"无权限"
3. **批量操作** - 用户可以同时选择多个终端组进行权限管理
4. **即时反馈** - 操作结果立即反映在界面上

## 核心API

### 1. 获取终端组树（带权限标识）
```
GET /terminal-group/tree/init
```
返回：完整的终端组树，每个节点标识当前用户组是否有权限

### 2. 批量更新权限
```
POST /terminal-group/permissions/batch-update
{
  "ugid": 123,
  "grantTerminalGroupIds": [1, 2, 3],  // 添加权限
  "revokeTerminalGroupIds": [4, 5, 6], // 移除权限
  "description": "批量更新权限"
}
```

## 前端交互逻辑

### 1. 权限管理页面结构
```html
<div class="permission-management">
  <div class="user-group-selector">
    <label>选择用户组:</label>
    <select v-model="selectedUserGroup">
      <option v-for="group in userGroups" :value="group.ugid">
        {{ group.name }}
      </option>
    </select>
  </div>
  
  <div class="terminal-group-tree">
    <h3>终端组权限管理</h3>
    <div class="tree-actions">
      <button @click="applyChanges" :disabled="!hasChanges">
        应用更改 ({{ changedCount }}项)
      </button>
    </div>
    
    <div class="tree-container">
      <terminal-group-node 
        v-for="node in terminalGroupTree" 
        :key="node.tgid"
        :node="node"
        @toggle-permission="onTogglePermission"
      />
    </div>
  </div>
</div>
```

### 2. 简化的树节点组件
```vue
<template>
  <div class="tree-node">
    <div class="node-content" :class="{ 'has-permission': node.hasPermission }">
      <div class="node-info">
        <span class="node-icon">📁</span>
        <span class="node-name">{{ node.tgName }}</span>
        <span v-if="node.hasPermission" class="permission-badge">✓</span>
      </div>
      
      <div class="node-actions">
        <button 
          @click="togglePermission"
          :class="node.hasPermission ? 'btn-revoke' : 'btn-grant'"
        >
          {{ node.hasPermission ? '移除权限' : '添加权限' }}
        </button>
      </div>
    </div>
    
    <div v-if="node.children && node.children.length > 0" class="node-children">
      <terminal-group-node 
        v-for="child in node.children" 
        :key="child.tgid"
        :node="child"
        @toggle-permission="$emit('toggle-permission', $event)"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'TerminalGroupNode',
  props: {
    node: {
      type: Object,
      required: true
    }
  },
  methods: {
    togglePermission() {
      this.$emit('toggle-permission', {
        tgid: this.node.tgid,
        hasPermission: this.node.hasPermission
      });
    }
  }
}
</script>
```

### 3. 权限管理逻辑
```javascript
class PermissionManager {
  constructor() {
    this.selectedUserGroup = null;
    this.terminalGroupTree = [];
    this.pendingChanges = {
      grant: [],    // 要添加权限的终端组ID
      revoke: []    // 要移除权限的终端组ID
    };
  }

  // 加载终端组树
  async loadTerminalGroupTree() {
    const response = await fetch('/terminal-group/tree/init');
    this.terminalGroupTree = await response.json();
  }

  // 切换权限状态
  onTogglePermission(event) {
    const { tgid, hasPermission } = event;
    
    if (hasPermission) {
      // 当前有权限，准备移除
      this.addToRevoke(tgid);
      this.removeFromGrant(tgid);
    } else {
      // 当前无权限，准备添加
      this.addToGrant(tgid);
      this.removeFromRevoke(tgid);
    }
    
    this.updateNodeStatus(tgid, !hasPermission);
  }

  // 应用更改
  async applyChanges() {
    if (!this.hasChanges()) return;
    
    const request = {
      ugid: this.selectedUserGroup.ugid,
      grantTerminalGroupIds: this.pendingChanges.grant,
      revokeTerminalGroupIds: this.pendingChanges.revoke,
      description: `批量更新权限：添加${this.pendingChanges.grant.length}项，移除${this.pendingChanges.revoke.length}项`
    };
    
    try {
      const response = await fetch('/terminal-group/permissions/batch-update', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });
      
      const result = await response.json();
      
      if (result.success) {
        this.showSuccessMessage(result.message);
        this.clearPendingChanges();
        await this.loadTerminalGroupTree(); // 重新加载
      } else {
        this.showErrorMessage(result.message);
      }
    } catch (error) {
      this.showErrorMessage('操作失败，请重试');
    }
  }

  // 辅助方法
  hasChanges() {
    return this.pendingChanges.grant.length > 0 || this.pendingChanges.revoke.length > 0;
  }

  get changedCount() {
    return this.pendingChanges.grant.length + this.pendingChanges.revoke.length;
  }

  addToGrant(tgid) {
    if (!this.pendingChanges.grant.includes(tgid)) {
      this.pendingChanges.grant.push(tgid);
    }
  }

  removeFromGrant(tgid) {
    const index = this.pendingChanges.grant.indexOf(tgid);
    if (index > -1) {
      this.pendingChanges.grant.splice(index, 1);
    }
  }

  addToRevoke(tgid) {
    if (!this.pendingChanges.revoke.includes(tgid)) {
      this.pendingChanges.revoke.push(tgid);
    }
  }

  removeFromRevoke(tgid) {
    const index = this.pendingChanges.revoke.indexOf(tgid);
    if (index > -1) {
      this.pendingChanges.revoke.splice(index, 1);
    }
  }

  updateNodeStatus(tgid, hasPermission) {
    const updateNode = (nodes) => {
      for (const node of nodes) {
        if (node.tgid === tgid) {
          node.hasPermission = hasPermission;
          return true;
        }
        if (node.children && updateNode(node.children)) {
          return true;
        }
      }
      return false;
    };
    
    updateNode(this.terminalGroupTree.accessibleTrees);
  }

  clearPendingChanges() {
    this.pendingChanges.grant = [];
    this.pendingChanges.revoke = [];
  }

  showSuccessMessage(message) {
    // 实现成功消息显示
    console.log('Success:', message);
  }

  showErrorMessage(message) {
    // 实现错误消息显示
    console.error('Error:', message);
  }
}
```

## 样式设计

```css
.permission-management {
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.user-group-selector {
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.terminal-group-tree {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  padding: 20px;
}

.tree-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.tree-node {
  margin-bottom: 8px;
}

.node-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  transition: all 0.2s;
}

.node-content.has-permission {
  background: #e8f5e8;
  border-color: #28a745;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.permission-badge {
  color: #28a745;
  font-weight: bold;
}

.btn-grant {
  background: #007bff;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-revoke {
  background: #dc3545;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.node-children {
  margin-left: 20px;
  border-left: 1px solid #e9ecef;
  padding-left: 20px;
}
```

## 关键特性

1. **用户友好的语言** - 使用"添加权限"/"移除权限"而不是"绑定"/"解绑"
2. **即时视觉反馈** - 权限状态通过颜色和图标直观显示
3. **批量操作** - 用户可以同时选择多个终端组进行操作
4. **操作预览** - 显示待处理的更改数量
5. **自动处理** - 后端自动处理复杂的绑定关系，用户无需了解细节

## 用户操作流程

1. 用户选择要管理的用户组
2. 系统显示终端组树，每个节点显示当前权限状态
3. 用户点击"添加权限"/"移除权限"按钮
4. 系统在本地标记更改，更新界面显示
5. 用户点击"应用更改"确认操作
6. 系统执行批量操作，自动处理复杂逻辑
7. 操作完成后更新界面显示

这样的设计完全隐藏了技术细节，用户只需要关心"权限管理"这个核心概念。