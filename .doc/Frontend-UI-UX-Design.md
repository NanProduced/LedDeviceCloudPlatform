# LedDeviceCloudPlatform 前端UI/UX设计文档

> **Sequential Thinking 设计方法论**
> 
> 本文档采用Sequential Thinking方法论，按照用户需求 → 信息架构 → 界面设计 → 交互设计 → 技术实现的顺序，逐步构建完整的前端UI/UX设计方案。

---

## 📊 Sequential Thinking Phase 1: 用户角色分析与需求挖掘

### 1.1 核心用户角色定义

基于项目的技术架构分析，我们识别出以下4个核心用户角色：

#### 🔹 系统管理员 (System Admin)
**权限级别**: `userType = 0` (超级管理员)
**主要职责**:
- 平台整体运维管理
- 跨组织的系统配置
- 全局权限策略管理
- 系统监控和性能优化

**关键需求**:
- 🎯 **全局视角**: 需要看到整个平台的运行状态
- 📊 **监控仪表板**: 实时的系统性能指标
- 🔧 **快速故障定位**: 当系统出问题时能快速找到原因
- 📈 **数据洞察**: 用户增长、设备在线率等关键指标

#### 🔹 组织管理员 (Organization Admin)
**权限级别**: `userType = 1` (组织管理员)
**主要职责**:
- 本组织内的用户管理
- LED设备的部署和配置
- 内容发布和管理
- 组织内权限分配

**关键需求**:
- 👥 **团队管理**: 高效的用户和角色管理界面
- 📱 **设备控制**: 直观的设备状态监控和控制面板
- 🎬 **内容管理**: 便捷的文件上传、转码、发布流程
- 📊 **运营数据**: 设备使用率、内容播放统计等

#### 🔹 普通用户 (Regular User)
**权限级别**: `userType = 2` (普通用户)
**主要职责**:
- 内容制作和上传
- 查看分配给自己的设备
- 执行获得授权的操作

**关键需求**:
- 📤 **简化上传**: 拖拽式文件上传，支持批量操作
- 👀 **权限明确**: 清楚地知道自己能做什么，不能做什么
- 🚀 **操作便捷**: 最少的点击次数完成常用操作
- 📱 **移动友好**: 支持手机端的基本操作

#### 🔹 LED设备 (Device Entity)
**系统角色**: 自动化实体
**技术特点**:
- 通过WebSocket保持与平台的实时连接
- 定期上报状态信息
- 接收和执行控制指令

**UI需求**:
- 🔴🟢 **状态可视化**: 在线/离线状态的直观显示
- 📊 **设备监控**: 实时的设备性能数据展示
- 🎛️ **远程控制**: 设备参数调整和控制面板

### 1.2 用户使用场景分析

#### 场景1: 日常运营监控
**用户**: 组织管理员
**频率**: 每天多次
**流程**: 登录 → 查看仪表板 → 检查设备状态 → 处理告警 → 查看内容播放情况

**UI需求**:
- 一屏式的核心信息展示
- 重要指标的醒目提醒
- 快速跳转到详细页面的入口

#### 场景2: 批量内容发布
**用户**: 普通用户
**频率**: 每周1-2次
**流程**: 选择文件 → 批量上传 → 等待转码 → 选择发布设备 → 确认发布

**UI需求**:
- 支持多文件同时拖拽上传
- 转码进度的实时显示
- 设备选择的便捷操作
- 发布状态的确认反馈

#### 场景3: 紧急故障处理
**用户**: 系统管理员
**频率**: 偶发但重要
**流程**: 接到告警 → 快速定位问题 → 查看相关日志 → 执行修复操作 → 验证恢复

**UI需求**:
- 告警信息的突出显示
- 一键跳转到问题详情
- 快速执行常见修复操作
- 操作结果的即时反馈

---

## 📐 Sequential Thinking Phase 2: 信息架构设计

### 2.1 导航架构设计

基于微服务架构和用户需求，设计三级导航结构：

```
📊 主控制台 (Dashboard)
├── 📈 运营概览 - 关键指标一览
├── 📊 实时监控 - 系统状态监控
└── 🚨 告警中心 - 异常情况处理

👥 用户管理 (User Management)
├── 👤 用户列表 - 用户CRUD操作
├── 👥 用户组管理 - 组织架构管理
├── 🏷️ 角色权限 - 角色分配和权限配置
└── 📊 用户分析 - 用户行为统计

🏢 组织管理 (Organization Management)
├── 🏗️ 组织架构 - 组织结构树形图
├── ⚙️ 组织配置 - 组织参数设置
└── 📈 组织统计 - 组织运营数据

📱 设备管理 (Device Management)
├── 📋 设备列表 - 设备状态和控制
├── 🌳 设备分组 - 设备组织结构
├── 📊 设备监控 - 实时状态监控
├── 🔧 设备配置 - 设备参数管理
└── 📈 设备统计 - 使用率和性能分析

📬 消息中心 (Message Center)
├── 💬 实时消息 - 即时通讯面板
├── 📢 广播通知 - 批量消息发送
├── 📝 消息模板 - 常用消息管理
└── 📊 消息统计 - 发送和接收统计

📁 文件管理 (File Management)
├── 📤 文件上传 - 单个和批量上传
├── 🗂️ 文件浏览 - 文件夹树形结构
├── 🎬 转码管理 - 视频转码任务
├── 👁️ 文件预览 - 文件预览和下载
└── 📊 存储统计 - 存储使用情况

⚙️ 系统管理 (System Management)
├── 🔐 权限策略 - Casbin策略配置
├── 🛠️ 系统配置 - 全局参数设置
├── 📋 审计日志 - 操作记录查询
└── 🔧 系统维护 - 数据库和缓存管理
```

### 2.2 页面层级结构

```
Level 1: 主框架页面
├─ 登录页面 (Login)
├─ 主应用框架 (Main App Layout)
│  ├─ 顶部导航栏 (Top Navigation)
│  ├─ 侧边菜单栏 (Sidebar Menu)
│  ├─ 内容区域 (Content Area)
│  └─ 状态栏 (Status Bar)

Level 2: 功能模块页面
├─ 控制台首页 (Dashboard Home)
├─ 用户管理首页 (User Management Home)
├─ 设备管理首页 (Device Management Home)
├─ 文件管理首页 (File Management Home)
└─ 系统管理首页 (System Management Home)

Level 3: 具体功能页面
├─ 用户列表页 (User List)
├─ 用户详情页 (User Detail)
├─ 设备监控页 (Device Monitor)
├─ 文件上传页 (File Upload)
└─ 系统日志页 (System Logs)

Level 4: 弹窗和组件
├─ 创建用户弹窗 (Create User Modal)
├─ 设备控制面板 (Device Control Panel)
├─ 文件预览组件 (File Preview Component)
└─ 权限配置组件 (Permission Config Component)
```

---

## 🎨 Sequential Thinking Phase 3: 视觉设计规范

### 3.1 设计语言定义

#### 🎨 设计理念
**专业性 + 现代感 + 效率导向**

- **专业性**: 体现LED显示行业的技术专业性
- **现代感**: 采用现代扁平化设计风格
- **效率导向**: 优化操作流程，减少用户认知负荷

#### 🎨 色彩系统

```css
/* 主色调 - 科技蓝 */
--primary-color: #1890ff;        /* 主要按钮、链接 */
--primary-light: #40a9ff;        /* 按钮悬停状态 */
--primary-dark: #096dd9;         /* 按钮按下状态 */

/* 辅助色 - 功能色彩 */
--success-color: #52c41a;        /* 成功状态、在线状态 */
--warning-color: #faad14;        /* 警告状态、待处理 */
--error-color: #f5222d;          /* 错误状态、离线状态 */
--info-color: #1890ff;           /* 信息提示 */

/* 中性色 - 界面基础色 */
--gray-1: #ffffff;               /* 纯白背景 */
--gray-2: #fafafa;               /* 浅灰背景 */
--gray-3: #f5f5f5;               /* 分割线、禁用背景 */
--gray-4: #d9d9d9;               /* 边框色 */
--gray-5: #bfbfbf;               /* 禁用文字 */
--gray-6: #8c8c8c;               /* 次要文字 */
--gray-7: #595959;               /* 主要文字 */
--gray-8: #262626;               /* 标题文字 */
--gray-9: #000000;               /* 纯黑文字 */

/* LED专业色 - 设备状态色 */
--led-online: #00ff00;           /* LED设备在线 */
--led-offline: #ff0000;          /* LED设备离线 */
--led-standby: #ffff00;          /* LED设备待机 */
--led-playing: #00ffff;          /* LED设备播放中 */
```

#### 📝 字体系统

```css
/* 字体族 */
--font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', 'Helvetica Neue', Helvetica, Arial, sans-serif;

/* 字体大小 */
--font-size-xs: 12px;            /* 辅助信息 */
--font-size-sm: 14px;            /* 正文 */
--font-size-md: 16px;            /* 小标题 */
--font-size-lg: 18px;            /* 中标题 */
--font-size-xl: 20px;            /* 大标题 */
--font-size-xxl: 24px;           /* 页面标题 */

/* 字体粗细 */
--font-weight-light: 300;
--font-weight-normal: 400;
--font-weight-medium: 500;
--font-weight-semibold: 600;
--font-weight-bold: 700;
```

#### 📏 间距系统

```css
/* 基础间距单位 */
--space-xs: 4px;                 /* 最小间距 */
--space-sm: 8px;                 /* 小间距 */
--space-md: 16px;                /* 标准间距 */
--space-lg: 24px;                /* 大间距 */
--space-xl: 32px;                /* 超大间距 */
--space-xxl: 48px;               /* 区块间距 */

/* 组件间距 */
--padding-xs: 4px 8px;           /* 小按钮内边距 */
--padding-sm: 8px 16px;          /* 标准按钮内边距 */
--padding-md: 12px 24px;         /* 大按钮内边距 */
--padding-lg: 16px 32px;         /* 卡片内边距 */
```

### 3.2 组件设计规范

#### 🔘 按钮组件规范

```css
/* 主要按钮 */
.btn-primary {
    background-color: var(--primary-color);
    border-color: var(--primary-color);
    color: #ffffff;
    padding: var(--padding-sm);
    border-radius: 6px;
    font-weight: var(--font-weight-medium);
    transition: all 0.3s ease;
}

.btn-primary:hover {
    background-color: var(--primary-light);
    border-color: var(--primary-light);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

/* 危险按钮 */
.btn-danger {
    background-color: var(--error-color);
    border-color: var(--error-color);
    color: #ffffff;
}

/* 设备状态按钮 */
.btn-device-online {
    background-color: var(--success-color);
    border-color: var(--success-color);
    color: #ffffff;
}

.btn-device-offline {
    background-color: var(--error-color);
    border-color: var(--error-color);
    color: #ffffff;
}
```

#### 📊 数据展示组件

```css
/* 统计卡片 */
.stat-card {
    background: var(--gray-1);
    border-radius: 8px;
    padding: var(--padding-lg);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
}

.stat-card:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
}

.stat-card .stat-number {
    font-size: var(--font-size-xxl);
    font-weight: var(--font-weight-bold);
    color: var(--primary-color);
    line-height: 1.2;
}

.stat-card .stat-label {
    font-size: var(--font-size-sm);
    color: var(--gray-6);
    margin-top: var(--space-xs);
}

/* 设备状态指示器 */
.device-status {
    display: inline-flex;
    align-items: center;
    gap: var(--space-xs);
    font-size: var(--font-size-sm);
}

.device-status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    animation: pulse 2s infinite;
}

.device-status-dot.online {
    background-color: var(--led-online);
}

.device-status-dot.offline {
    background-color: var(--led-offline);
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}
```

---

## 🖼️ Sequential Thinking Phase 4: 核心页面设计

### 4.1 登录页面设计

#### 🎯 设计目标
- 体现平台的专业性和科技感
- 支持OAuth2认证流程
- 兼容移动端访问

#### 🎨 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│                        顶部Logo区域                           │
│              LedDeviceCloudPlatform                         │
│                    智能LED云控平台                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│     ┌─────────────────────────────────────────────────┐     │
│     │                登录表单区域                      │     │
│     │                                                 │     │
│     │  🔑 用户名/邮箱                                    │     │
│     │  ┌─────────────────────────────────────────┐    │     │
│     │  │ username@example.com                   │    │     │
│     │  └─────────────────────────────────────────┘    │     │
│     │                                                 │     │
│     │  🔒 密码                                        │     │
│     │  ┌─────────────────────────────────────────┐    │     │
│     │  │ ●●●●●●●●                               │    │     │
│     │  └─────────────────────────────────────────┘    │     │
│     │                                                 │     │
│     │  ☑️ 记住我        忘记密码?                       │     │
│     │                                                 │     │
│     │  ┌─────────────────────────────────────────┐    │     │
│     │  │            🚀 登 录                     │    │     │
│     │  └─────────────────────────────────────────┘    │     │
│     │                                                 │     │
│     │            ── 或者使用 ──                        │     │
│     │                                                 │     │
│     │  🔗 OAuth2企业登录   📱 移动端扫码登录            │     │
│     └─────────────────────────────────────────────────┘     │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│              © 2024 智能LED云控平台. 保留所有权利             │
└─────────────────────────────────────────────────────────────┘
```

#### 💡 关键设计要素

1. **科技感背景**: 使用LED点阵动画或粒子效果
2. **响应式布局**: 移动端自动调整为垂直布局
3. **加载状态**: 登录按钮点击后显示加载动画
4. **错误提示**: 友好的错误信息展示
5. **安全性**: 密码输入框的遮掩处理

### 4.2 主控制台页面设计

#### 🎯 设计目标
- 提供系统运行状态的全貌视图
- 支持实时数据更新
- 快速访问常用功能

#### 🎨 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│ 🏠 首页 > 📊 控制台                           👤 管理员 ⚙️   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 📈 关键指标概览                                              │
│ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐   │
│ │   1,234   │ │    456    │ │    89%    │ │    12     │   │
│ │ 📱 在线设备 │ │ 👥 活跃用户 │ │ 📊 系统可用率 │ │ 🚨 待处理告警 │   │
│ └───────────┘ └───────────┘ └───────────┘ └───────────┘   │
│                                                             │
│ ┌─────────────────────────┐ ┌─────────────────────────────┐ │
│ │     📊 实时监控图表      │ │        🎯 快捷操作          │ │
│ │                        │ │                             │ │
│ │  设备在线率 [折线图]     │ │  🔘 创建用户                │ │
│ │  系统负载 [面积图]       │ │  🔘 添加设备                │ │
│ │  消息推送量 [柱状图]     │ │  🔘 上传文件                │ │
│ │                        │ │  🔘 发送通知                │ │
│ │                        │ │  🔘 查看日志                │ │
│ └─────────────────────────┘ └─────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                   🌍 设备地图分布                        │ │
│ │                                                         │ │
│ │     [互动式地图，显示各地区设备分布和状态]                 │ │
│ │                                                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────┐ ┌─────────────────────────────┐ │
│ │     📋 最近活动         │ │        🚨 系统告警          │ │
│ │                        │ │                             │ │
│ │ • 用户张三创建了设备A   │ │ ⚠️ 服务器CPU使用率过高       │ │
│ │ • 设备B开始播放内容    │ │ 🔴 设备C连接超时           │ │
│ │ • 管理员发布了公告      │ │ ⚠️ 存储空间不足80%          │ │
│ │ • 转码任务完成         │ │ 🟡 消息队列积压             │ │
│ │                        │ │                             │ │
│ └─────────────────────────┘ └─────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### 📊 实时数据更新机制

```javascript
// WebSocket连接示例
const dashboardWS = new WebSocket('ws://localhost:8084/ws/dashboard');

dashboardWS.onmessage = function(event) {
    const data = JSON.parse(event.data);
    
    switch(data.type) {
        case 'DEVICE_STATUS_UPDATE':
            updateDeviceCount(data.onlineCount, data.totalCount);
            updateDeviceMap(data.deviceLocations);
            break;
        case 'SYSTEM_METRICS':
            updateSystemMetrics(data.metrics);
            break;
        case 'ALERT_NOTIFICATION':
            showAlert(data.alert);
            break;
    }
};
```

### 4.3 设备管理页面设计

#### 🎯 设计目标
- 支持10k+设备的高性能显示
- 提供多种视图模式
- 实时设备状态监控

#### 🎨 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│ 🏠 首页 > 📱 设备管理                        🔍 搜索框 ⚙️   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 📊 设备概览    📋 1,234台 🟢 1,120在线 🔴 114离线           │
│                                                             │
│ ┌─────────────────┐ ┌─────────────────────────────────────┐ │
│ │   🌳 设备分组    │ │           📱 设备列表                │ │
│ │                │ │                                     │ │
│ │ 📁 总公司       │ │ 视图模式: [🔘表格] 📋列表 🖼️网格     │ │
│ │ ├─ 📁 北京分公司 │ │                                     │ │
│ │ │  ├─ 🏢 办公楼 │ │ 筛选: [全部▼] [在线▼] [品牌▼]      │ │
│ │ │  └─ 🏪 展厅   │ │                                     │ │
│ │ ├─ 📁 上海分公司 │ │ ╔═══════════════════════════════════╗ │
│ │ │  └─ 🏬 商场   │ │ ║ 设备名称 │状态│位置│型号│最后在线 ║ │
│ │ └─ 📁 深圳分公司 │ │ ╠═══════════════════════════════════╣ │
│ │                │ │ ║LED-001  │🟢  │北京│P4 │2分钟前  ║ │
│ │ 🔄 实时同步     │ │ ║LED-002  │🔴  │上海│P6 │10分钟前 ║ │
│ │                │ │ ║LED-003  │🟢  │深圳│P4 │1分钟前  ║ │
│ └─────────────────┘ │ ║...                              ║ │
│                     │ ╚═══════════════════════════════════╝ │
│                     │                                     │ │
│                     │ 📄 分页: 1/25 页 [<] [1][2][3] [>] │ │
│                     └─────────────────────────────────────┘ │
│                                                             │
│ 🛠️ 批量操作:                                                │
│ [✓选中3台设备] 🔘重启设备 🔘推送内容 🔘更新配置             │
└─────────────────────────────────────────────────────────────┘
```

#### 📱 设备详情弹窗设计

```
┌─────────────────────────────────────────────────────────────┐
│ 📱 设备详情 - LED-001                              ✖️ 关闭   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ┌─────────────────────────┐ ┌─────────────────────────────┐ │
│ │     📊 基本信息         │ │        🔧 实时控制          │ │
│ │                        │ │                             │ │
│ │ 🏷️ 名称: LED-001        │ │ 💡 亮度: [████████░░] 80%   │ │
│ │ 📍 位置: 北京-办公楼A   │ │ 🔊 音量: [██████░░░░] 60%   │ │
│ │ 🏭 型号: P4室内全彩     │ │ 🌡️ 温度: 45°C              │ │
│ │ 📺 分辨率: 1920x1080   │ │                             │ │
│ │ 🔗 IP: 192.168.1.100   │ │ 🔘 开机   🔘 关机           │ │
│ │ ⏰ 最后在线: 2分钟前   │ │ 🔘 重启   🔘 截屏           │ │
│ └─────────────────────────┘ └─────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                    📈 性能监控                           │ │
│ │                                                         │ │
│ │ CPU: [██████░░░░] 60%    内存: [████████░░] 80%         │ │
│ │ 网络: ⬆️ 1.2MB/s ⬇️ 850KB/s                             │ │
│ │                                                         │ │
│ │ [实时性能图表显示区域]                                    │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────┐ ┌─────────────────────────────┐ │
│ │     📋 播放内容         │ │        📊 操作日志          │ │
│ │                        │ │                             │ │
│ │ 🎬 当前播放:            │ │ 2024-07-26 12:00 开机      │ │
│ │    宣传视频.mp4        │ │ 2024-07-26 11:30 推送内容  │ │
│ │ ⏱️ 播放时长: 02:35/05:20│ │ 2024-07-26 10:15 调整亮度  │ │
│ │ 🔄 循环播放: 开启       │ │ 2024-07-26 09:45 在线检测  │ │
│ │                        │ │                             │ │
│ └─────────────────────────┘ └─────────────────────────────┘ │
│                                                             │
│               🔘 保存配置    🔘 导出报告                     │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 文件管理页面设计

#### 🎯 设计目标
- 支持大文件和批量操作
- 实时转码进度监控
- 多种文件预览方式

#### 🎨 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│ 🏠 首页 > 📁 文件管理                       🔍 搜索 | 📤 上传 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ┌─────────────────┐ ┌─────────────────────────────────────┐ │
│ │   📂 文件夹结构  │ │            📁 文件列表              │ │
│ │                │ │                                     │ │
│ │ 📁 根目录       │ │ 路径: 根目录 > 视频素材              │ │
│ │ ├─ 📁 视频素材  │ │                                     │ │
│ │ │  ├─ 📁 广告片 │ │ 视图: [🔘缩略图] 📄列表  排序: 时间▼ │ │
│ │ │  └─ 📁 宣传片 │ │                                     │ │
│ │ ├─ 📁 图片素材  │ │ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │ │
│ │ │  ├─ 📁 海报   │ │ │[📷] │ │[🎬] │ │[🎬] │ │[📷] │   │ │
│ │ │  └─ 📁 背景   │ │ │pic1 │ │vid1 │ │vid2 │ │pic2 │   │ │
│ │ └─ 📁 音频素材  │ │ │2MB  │ │50MB │ │120MB│ │5MB  │   │ │
│ │                │ │ │✓   │ │🔄转码│ │✅   │ │✓   │   │ │
│ │ 💾 总容量: 2TB  │ │ └─────┘ └─────┘ └─────┘ └─────┘   │ │
│ │ 📊 已用: 850GB  │ │                                     │ │
│ │ 📈 可用: 1.2TB  │ │ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐   │ │
│ └─────────────────┘ │ │[🎵] │ │[📄] │ │[📁] │ │     │   │ │
│                     │ │aud1 │ │doc1 │ │新建 │ │     │   │ │
│                     │ │10MB │ │1MB  │ │文件夹│ │     │   │ │
│                     │ │✓   │ │✓   │ │     │ │     │   │ │
│                     │ └─────┘ └─────┘ └─────┘ └─────┘   │ │
│                     └─────────────────────────────────────┘ │
│                                                             │
│ 🛠️ 操作工具栏:                                              │
│ [✓已选3个文件] 📤上传 📥下载 ✂️剪切 📋复制 🗑️删除 🏷️标签     │
└─────────────────────────────────────────────────────────────┘
```

#### 🎬 转码进度监控面板

```
┌─────────────────────────────────────────────────────────────┐
│ 🎬 转码任务监控                                    ⚙️ 设置   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 📊 队列状态: 🟢 正常运行  当前队列: 3个任务  历史成功率: 98.5% │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                    🚀 进行中任务                         │ │
│ │                                                         │ │
│ │ 📁 宣传视频_2024.mp4 → H.264_1080p                      │ │
│ │ ████████████████████████████████████████████░░░ 85%     │ │
│ │ ⏱️ 已用时: 5分30秒  ⏳ 预计剩余: 1分钟  🚀 速度: 2.5x    │ │
│ │ 📊 当前: 02:35/05:20  🎯 输出大小: 45MB                │ │
│ │                                                         │ │
│ │ [🔴 停止] [⏸️ 暂停]  📊 详细日志                        │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                    ⏳ 等待队列                           │ │
│ │                                                         │ │
│ │ 1️⃣ 产品展示_final.mov → WebM_720p         [⬆️ 优先]     │ │
│ │ 2️⃣ 活动回顾.avi → MP4_4K                 [❌ 取消]     │ │
│ │                                                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                    ✅ 已完成任务                         │ │
│ │                                                         │ │
│ │ ✅ 开场动画.mp4 → H.264_1080p    ⏱️ 3分钟  📥 下载      │ │
│ │ ✅ 公司介绍.mov → WebM_720p      ⏱️ 2分钟  📥 下载      │ │
│ │ ❌ 年会视频.avi → MP4_4K         ❌ 转码失败 🔄 重试      │ │
│ │                                                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│                       🔧 转码设置 📊 性能统计                │
└─────────────────────────────────────────────────────────────┘
```

### 4.5 消息中心页面设计

#### 🎯 设计目标
- 实时消息推送和通知
- 支持多种消息类型
- 便捷的群发和广播功能

#### 🎨 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│ 🏠 首页 > 📬 消息中心                       🔔 通知 | ⚙️ 设置 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ┌─────────────────┐ ┌─────────────────────────────────────┐ │
│ │   💬 会话列表    │ │            📝 消息内容              │ │
│ │                │ │                                     │ │
│ │ 🔍 搜索会话...  │ │ 📢 系统广播                          │ │
│ │                │ │                                     │ │
│ │ 📢 [3] 系统通知 │ │ ┌─────────────────────────────────┐ │ │
│ │ 👥 [12] 全体用户│ │ │ 🤖 系统         12:30           │ │
│ │ 🏢 [5] 北京分部 │ │ │ 📢 系统维护通知:                │ │
│ │ 🏢 [8] 上海分部 │ │ │ 今晚23:00-01:00进行系统维护     │ │
│ │ 👤 张三 [1]     │ │ │ 请提前保存工作内容               │ │
│ │ 👤 李四         │ │ └─────────────────────────────────┘ │ │
│ │ 👤 王五 [2]     │ │                                     │ │
│ │                │ │ ┌─────────────────────────────────┐ │ │
│ │ 📊 在线: 156人  │ │ │ 👤 张三         11:45           │ │
│ │ 📊 总计: 234人  │ │ │ 💬 LED-001设备出现故障，         │ │
│ │                │ │ │ 无法正常显示内容                 │ │
│ │ 🔄 实时同步     │ │ └─────────────────────────────────┘ │ │
│ └─────────────────┘ │                                     │ │
│                     │ ┌─────────────────────────────────┐ │ │
│                     │ │ 👤 我            11:50           │ │
│                     │ │ 💬 已收到，正在处理中...         │ │
│                     │ │                             ✓✓ │ │
│                     │ └─────────────────────────────────┘ │ │
│                     │                                     │ │
│                     │ 📝 输入框...                 [📎][📤] │ │
│                     └─────────────────────────────────────┘ │
│                                                             │
│ 🛠️ 快捷功能:                                                │
│ 📢 群发通知  🚨 紧急广播  📊 消息统计  🗂️ 消息模板         │
└─────────────────────────────────────────────────────────────┘
```

#### 📢 消息发送弹窗设计

```
┌─────────────────────────────────────────────────────────────┐
│ 📢 发送消息                                        ✖️ 关闭   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 🎯 发送目标:                                                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📻 消息类型: [系统通知 ▼]                                │ │
│ │                                                         │ │
│ │ 👥 接收对象:                                            │ │
│ │ ☑️ 全体用户 (234人)                                     │ │
│ │ ☑️ 📱 北京分部 (89人)                                   │ │
│ │ ☐ 📱 上海分部 (76人)                                   │ │
│ │ ☐ 📱 深圳分部 (69人)                                   │ │
│ │                                                         │ │
│ │ 🏷️ 自定义标签: [运维人员] [设备管理员]                   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ 📝 消息内容:                                                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🏷️ 标题: 系统维护通知                                   │ │
│ │                                                         │ │
│ │ 📄 内容:                                                │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 各位用户：                                          │ │ │
│ │ │                                                     │ │ │
│ │ │ 为了提升系统性能，计划于今晚23:00-01:00             │ │ │
│ │ │ 进行系统维护升级。维护期间系统将暂停服务，           │ │ │
│ │ │ 请大家提前做好相关工作安排。                         │ │ │
│ │ │                                                     │ │ │
│ │ │ 如有紧急情况，请联系：admin@company.com             │ │ │
│ │ │                                                     │ │ │
│ │ │ 感谢理解与配合！                                     │ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ⚙️ 发送选项:                                                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ⏰ 发送时间: [🔘立即发送] ⏰定时发送                      │ │
│ │ 🔔 推送方式: ☑️网页通知 ☑️邮件 ☐短信                   │ │
│ │ 📊 优先级别: [🔘普通] 🔸重要 🔴紧急                      │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ 📋 预计接收: 155人    📊 字符数: 128/500                    │
│                                                             │
│            🔘 保存模板    🔘 预览    📤 发送                │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚡ Sequential Thinking Phase 5: 交互设计与用户体验

### 5.1 操作流程设计

#### 🚀 核心操作流程优化

##### 1️⃣ 快速设备添加流程

```
用户意图: 快速添加新的LED设备
传统流程: 6步操作，需要2分钟
优化流程: 3步操作，需要30秒

🎯 优化后流程:
步骤1: 点击"添加设备"浮动按钮
     → 弹出智能表单（自动检测网络中的设备）
     
步骤2: 选择检测到的设备或手动输入
     → 自动填充设备信息（型号、分辨率等）
     
步骤3: 选择设备组，点击确认
     → 自动配置，设备立即上线

💡 优化要点:
- 使用浮动操作按钮(FAB)提供快速入口
- 智能表单减少用户输入
- 自动检测和配置降低技术门槛
- 实时反馈设备连接状态
```

##### 2️⃣ 批量内容发布流程

```
用户意图: 将新内容推送到多个设备
传统流程: 多页面跳转，容易遗漏设备
优化流程: 单页面完成，拖拽式操作

🎯 优化后界面:
┌─────────────────────────────────────────────────────────────┐
│                  📤 批量内容发布向导                         │
│                                                             │
│ 步骤 1/3: 选择内容                                          │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐                       │
│ │ [🎬]    │ │ [📷]    │ │ [🎵]    │                       │
│ │ 视频A   │ │ 图片B   │ │ 音频C   │ ← 可多选               │
│ │ ✓选中   │ │         │ │         │                       │
│ └─────────┘ └─────────┘ └─────────┘                       │
│                                                             │
│ 步骤 2/3: 选择设备 (拖拽设备到右侧区域)                      │
│ 🏢 设备列表          │         📱 发布目标                  │
│ LED-001 🟢          │         LED-001 🎬 视频A             │
│ LED-002 🟢          │    →    LED-003 🎬 视频A             │
│ LED-003 🔴          └────→    LED-005 🎬 视频A             │
│ LED-004 🟢                                                 │
│ LED-005 🟢                   📊 共3台设备，预计1分钟       │
│                                                             │
│ 步骤 3/3: 发布设置                                          │
│ ⏰ 立即发布 📅 定时发布  🔄 循环播放  📊 播放报告           │
│                                                             │
│              🔙 上一步        📤 开始发布                   │
└─────────────────────────────────────────────────────────────┘

💡 交互亮点:
- 向导式步骤，清晰的进度指示
- 拖拽操作，直观的设备分配
- 实时计算发布时间和资源消耗
- 支持一键选择设备组
```

### 5.2 响应式设计策略

#### 📱 移动端适配方案

##### 导航结构适配

```css
/* 桌面端 - 侧边栏导航 */
@media (min-width: 1024px) {
    .layout {
        display: grid;
        grid-template-columns: 250px 1fr;
        grid-template-rows: 60px 1fr;
        grid-template-areas: 
            "sidebar header"
            "sidebar content";
    }
    
    .sidebar {
        grid-area: sidebar;
        position: fixed;
        transform: translateX(0);
    }
}

/* 移动端 - 底部导航 */
@media (max-width: 768px) {
    .layout {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: 60px 1fr 60px;
        grid-template-areas: 
            "header"
            "content"
            "bottom-nav";
    }
    
    .bottom-nav {
        grid-area: bottom-nav;
        display: flex;
        justify-content: space-around;
        align-items: center;
        background: var(--gray-1);
        border-top: 1px solid var(--gray-4);
    }
    
    .nav-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        padding: 8px;
        font-size: 12px;
    }
}
```

##### 核心功能移动端优化

```
📱 移动端主控制台设计:

┌─────────────────────────────────────┐
│ 🏠 LED云控平台        🔔 📱 👤      │ ← 顶部栏简化
├─────────────────────────────────────┤
│                                     │
│ 📈 今日概况                          │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │   156   │ │   89%   │ │   12    │ │ ← 关键指标卡片
│ │ 在线设备 │ │ 可用率  │ │ 告警    │ │
│ └─────────┘ └─────────┘ └─────────┘ │
│                                     │
│ 🚨 重要告警                          │
│ • 设备LED-001离线 [查看]             │
│ • 存储空间不足 [处理]                │
│                                     │
│ 🛠️ 快捷操作                          │
│ [📤 上传文件] [📱 添加设备]           │
│ [📢 发送通知] [📊 查看报告]           │
│                                     │
│ 📋 最近活动                          │
│ • 张三上传了视频                     │
│ • 设备LED-002开始播放               │
│                                     │
└─────────────────────────────────────┘
│ 🏠 📱 📬 📁 ⚙️                      │ ← 底部导航
└─────────────────────────────────────┘
```

### 5.3 实时性功能的UI处理

#### 🔄 WebSocket状态管理

```javascript
// WebSocket连接状态UI组件
class WebSocketStatus extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            connectionStatus: 'connecting', // connecting, connected, disconnected, error
            lastHeartbeat: null,
            reconnectAttempts: 0
        };
    }

    render() {
        const { connectionStatus, reconnectAttempts } = this.state;
        
        return (
            <div className="websocket-status">
                {connectionStatus === 'connected' && (
                    <span className="status-indicator online">
                        🔗 实时连接正常
                    </span>
                )}
                
                {connectionStatus === 'connecting' && (
                    <span className="status-indicator connecting">
                        🔄 正在连接... ({reconnectAttempts > 0 ? `重试${reconnectAttempts}` : ''})
                    </span>
                )}
                
                {connectionStatus === 'disconnected' && (
                    <span className="status-indicator offline">
                        ⚠️ 连接断开，正在重连...
                        <button onClick={this.forceReconnect}>立即重连</button>
                    </span>
                )}
            </div>
        );
    }
}
```

#### 📊 实时数据更新动画

```css
/* 数据更新动画效果 */
@keyframes dataUpdate {
    0% { 
        background-color: var(--primary-color);
        transform: scale(1);
    }
    50% { 
        background-color: var(--primary-light);
        transform: scale(1.05);
    }
    100% { 
        background-color: transparent;
        transform: scale(1);
    }
}

.stat-card.updating {
    animation: dataUpdate 0.6s ease-in-out;
}

/* 设备状态变化动画 */
.device-status-dot.status-change {
    animation: statusPulse 1s ease-in-out 3;
}

@keyframes statusPulse {
    0%, 100% { 
        transform: scale(1); 
        opacity: 1; 
    }
    50% { 
        transform: scale(1.3); 
        opacity: 0.7; 
    }
}
```

### 5.4 权限控制的UI实现

#### 🔐 基于权限的组件渲染

```javascript
// 权限控制高阶组件
const withPermission = (WrappedComponent, requiredPermission) => {
    return class PermissionWrapper extends React.Component {
        constructor(props) {
            super(props);
            this.state = {
                hasPermission: false,
                loading: true
            };
        }

        async componentDidMount() {
            try {
                const userPermissions = await this.getUserPermissions();
                const hasPermission = this.checkPermission(
                    userPermissions, 
                    requiredPermission
                );
                
                this.setState({ 
                    hasPermission, 
                    loading: false 
                });
            } catch (error) {
                this.setState({ 
                    hasPermission: false, 
                    loading: false 
                });
            }
        }

        checkPermission(userPermissions, required) {
            // 基于Casbin的权限检查逻辑
            const { subject, domain, url, action } = required;
            return userPermissions.some(perm => 
                perm.subject === subject &&
                perm.domain === domain &&
                perm.url === url &&
                perm.action === action &&
                perm.effect === 'allow'
            );
        }

        render() {
            const { loading, hasPermission } = this.state;
            
            if (loading) {
                return <div className="permission-loading">加载中...</div>;
            }
            
            if (!hasPermission) {
                return (
                    <div className="permission-denied">
                        <span>🔒 您没有权限访问此功能</span>
                        <button onClick={this.requestPermission}>
                            申请权限
                        </button>
                    </div>
                );
            }
            
            return <WrappedComponent {...this.props} />;
        }
    };
};

// 使用示例
const ProtectedDeviceControl = withPermission(
    DeviceControlPanel,
    {
        subject: 'user_123',
        domain: 'org_456', 
        url: '/device/control',
        action: 'POST'
    }
);
```

#### 🎨 权限级别的视觉区分

```css
/* 不同权限级别的视觉样式 */
.user-role-admin {
    border-left: 4px solid var(--error-color);
}

.user-role-admin .user-avatar::after {
    content: '👑';
    position: absolute;
    top: -5px;
    right: -5px;
    font-size: 12px;
}

.user-role-manager {
    border-left: 4px solid var(--warning-color);
}

.user-role-user {
    border-left: 4px solid var(--success-color);
}

/* 功能按钮的权限状态 */
.btn-permission-denied {
    background-color: var(--gray-4);
    color: var(--gray-5);
    cursor: not-allowed;
    opacity: 0.6;
}

.btn-permission-denied:hover {
    background-color: var(--gray-4);
    transform: none;
    box-shadow: none;
}

/* 权限提示工具条 */
.permission-tooltip {
    position: relative;
}

.permission-tooltip:hover::after {
    content: attr(data-permission-hint);
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    background: var(--gray-8);
    color: white;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 12px;
    whitespace: nowrap;
    z-index: 1000;
}
```

---

## 🔧 Sequential Thinking Phase 6: 技术实现方案

### 6.1 前端技术栈选型

#### 🚀 核心技术框架

```json
{
  "framework": {
    "name": "React 18.2+",
    "reason": "组件化开发，优秀的生态系统，支持并发特性",
    "alternatives": ["Vue 3", "Angular 14+"]
  },
  "stateManagement": {
    "name": "Redux Toolkit + RTK Query",
    "reason": "强大的状态管理，内置数据获取和缓存",
    "alternatives": ["Zustand", "Jotai", "SWR + useState"]
  },
  "routing": {
    "name": "React Router 6",
    "reason": "声明式路由，支持懒加载和嵌套路由",
    "features": ["code-splitting", "nested-routing", "lazy-loading"]
  },
  "uiFramework": {
    "name": "Ant Design 5.0+",
    "reason": "企业级UI组件库，丰富的组件生态",
    "customization": "基于Design Token的主题定制"
  },
  "dataVisualization": {
    "name": "ECharts + AntV/G2",
    "reason": "强大的图表能力，支持大数据量渲染",
    "useCases": ["实时监控图表", "设备分布地图", "性能统计图"]
  },
  "realtime": {
    "name": "WebSocket + EventSource",
    "reason": "双向实时通信 + 服务器推送事件",
    "fallback": "Long Polling"
  }
}
```

#### 📦 项目结构设计

```
src/
├── components/           # 公共组件
│   ├── common/          # 通用组件
│   │   ├── Layout/      # 布局组件
│   │   ├── Charts/      # 图表组件
│   │   └── Forms/       # 表单组件
│   ├── business/        # 业务组件
│   │   ├── DeviceCard/  # 设备卡片
│   │   ├── UserAvatar/  # 用户头像
│   │   └── FileUploader/ # 文件上传器
│   └── ui/              # UI基础组件
│       ├── Button/      # 按钮组件
│       ├── Modal/       # 弹窗组件
│       └── Table/       # 表格组件
├── pages/               # 页面组件
│   ├── Dashboard/       # 控制台页面
│   ├── UserManagement/  # 用户管理
│   ├── DeviceManagement/ # 设备管理
│   ├── FileManagement/  # 文件管理
│   └── MessageCenter/   # 消息中心
├── services/            # API服务层
│   ├── api/            # API接口定义
│   ├── websocket/      # WebSocket服务
│   └── auth/           # 认证服务
├── store/              # 状态管理
│   ├── slices/         # Redux切片
│   ├── middleware/     # 中间件
│   └── selectors/      # 选择器
├── hooks/              # 自定义Hooks
│   ├── useWebSocket.js # WebSocket Hook
│   ├── usePermission.js # 权限Hook
│   └── useRealtime.js  # 实时数据Hook
├── utils/              # 工具函数
│   ├── request.js      # HTTP请求工具
│   ├── auth.js         # 认证工具
│   ├── permission.js   # 权限工具
│   └── format.js       # 格式化工具
├── styles/             # 样式文件
│   ├── themes/         # 主题文件
│   ├── components/     # 组件样式
│   └── globals/        # 全局样式
└── types/              # TypeScript类型定义
    ├── api.ts          # API类型
    ├── user.ts         # 用户类型
    └── device.ts       # 设备类型
```

### 6.2 API集成方案

#### 🔗 统一API客户端

```javascript
// services/api/client.js
import axios from 'axios';
import { store } from '../store';
import { logout } from '../store/slices/authSlice';

class ApiClient {
    constructor() {
        this.baseURL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8082';
        this.client = axios.create({
            baseURL: this.baseURL,
            timeout: 30000,
            headers: {
                'Content-Type': 'application/json',
            }
        });

        this.setupInterceptors();
    }

    setupInterceptors() {
        // 请求拦截器 - 添加认证token
        this.client.interceptors.request.use(
            (config) => {
                const state = store.getState();
                const token = state.auth.token;
                
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
                
                // 添加组织ID到请求头
                const organizationId = state.auth.user?.organizationId;
                if (organizationId) {
                    config.headers['X-Organization-Id'] = organizationId;
                }
                
                return config;
            },
            (error) => Promise.reject(error)
        );

        // 响应拦截器 - 处理通用错误
        this.client.interceptors.response.use(
            (response) => {
                // 检查业务状态码
                if (response.data && response.data.code !== undefined) {
                    if (response.data.code !== 200) {
                        throw new Error(response.data.message || '请求失败');
                    }
                    return response.data.data; // 直接返回业务数据
                }
                return response.data;
            },
            (error) => {
                if (error.response?.status === 401) {
                    store.dispatch(logout());
                    window.location.href = '/login';
                }
                
                throw new Error(
                    error.response?.data?.message || 
                    error.message || 
                    '网络请求失败'
                );
            }
        );
    }

    // 用户管理API
    user = {
        getCurrent: () => this.client.get('/user/current'),
        create: (userData) => this.client.post('/user/create', userData),
        updatePassword: (passwordData) => this.client.post('/user/modify/pwd', passwordData),
        assignRoles: (data) => this.client.post('/user/assign-roles', data),
        list: (params) => this.client.post('/user-group/list', params),
    };

    // 设备管理API
    device = {
        create: (deviceData) => this.client.post('/terminal/create', deviceData),
        getGroups: () => this.client.get('/terminal-group/tree/init'),
        createGroup: (groupData) => this.client.post('/terminal-group/create', groupData),
        updateGroup: (groupData) => this.client.post('/terminal-group/update', groupData),
        deleteGroup: (groupData) => this.client.post('/terminal-group/delete', groupData),
    };

    // 文件管理API
    file = {
        upload: (formData, onProgress) => this.client.post('/file/upload/single', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            onUploadProgress: onProgress
        }),
        list: (params) => this.client.post('/file/management/list', params),
        delete: (fileId) => this.client.delete(`/file/management/${fileId}`),
        getInfo: (fileId) => this.client.get(`/file/management/${fileId}`),
    };

    // 转码管理API
    transcoding = {
        submit: (taskData) => this.client.post('/file/transcoding/submit', taskData),
        getProgress: (taskId) => this.client.get(`/file/transcoding/progress/${taskId}`),
        cancel: (taskId) => this.client.delete(`/file/transcoding/cancel/${taskId}`),
        getPresets: () => this.client.get('/file/transcoding/presets'),
    };

    // 消息中心API
    message = {
        send: (userId, messageData) => this.client.post(`/api/v1/messages/send/${userId}`, messageData),
        broadcast: (orgId, messageData) => this.client.post(`/api/v1/messages/broadcast/organization/${orgId}`, messageData),
        getStatus: (userId) => this.client.get(`/api/v1/messages/status/user/${userId}`),
        getStatistics: () => this.client.get('/api/v1/messages/statistics'),
    };
}

export const apiClient = new ApiClient();
```

### 6.3 WebSocket集成方案

#### 🔌 WebSocket管理器

```javascript
// services/websocket/WebSocketManager.js
class WebSocketManager {
    constructor() {
        this.connections = new Map();
        this.listeners = new Map();
        this.reconnectAttempts = new Map();
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
    }

    // 创建WebSocket连接
    connect(name, url, protocols = []) {
        if (this.connections.has(name)) {
            this.disconnect(name);
        }

        const ws = new WebSocket(url, protocols);
        const connectionInfo = {
            ws,
            url,
            protocols,
            status: 'connecting',
            lastHeartbeat: Date.now()
        };

        this.connections.set(name, connectionInfo);
        this.setupEventHandlers(name, ws);
        
        return ws;
    }

    // 设置事件处理器
    setupEventHandlers(name, ws) {
        ws.onopen = (event) => {
            console.log(`WebSocket连接已建立: ${name}`);
            this.updateConnectionStatus(name, 'connected');
            this.resetReconnectAttempts(name);
            this.emit(name, 'connected', event);
        };

        ws.onmessage = (event) => {
            const connectionInfo = this.connections.get(name);
            if (connectionInfo) {
                connectionInfo.lastHeartbeat = Date.now();
            }

            try {
                const data = JSON.parse(event.data);
                this.emit(name, 'message', data);
                
                // 根据消息类型分发到具体处理器
                if (data.type) {
                    this.emit(name, data.type, data);
                }
            } catch (error) {
                console.error('WebSocket消息解析错误:', error);
                this.emit(name, 'error', error);
            }
        };

        ws.onclose = (event) => {
            console.log(`WebSocket连接已关闭: ${name}`, event);
            this.updateConnectionStatus(name, 'disconnected');
            this.emit(name, 'disconnected', event);
            
            // 非正常关闭时自动重连
            if (event.code !== 1000) {
                this.handleReconnect(name);
            }
        };

        ws.onerror = (error) => {
            console.error(`WebSocket连接错误: ${name}`, error);
            this.updateConnectionStatus(name, 'error');
            this.emit(name, 'error', error);
        };
    }

    // 处理重连逻辑
    handleReconnect(name) {
        const attempts = this.reconnectAttempts.get(name) || 0;
        
        if (attempts < this.maxReconnectAttempts) {
            const delay = this.reconnectDelay * Math.pow(2, attempts); // 指数退避
            
            setTimeout(() => {
                console.log(`尝试重连WebSocket: ${name} (${attempts + 1}/${this.maxReconnectAttempts})`);
                const connectionInfo = this.connections.get(name);
                
                if (connectionInfo) {
                    this.reconnectAttempts.set(name, attempts + 1);
                    this.connect(name, connectionInfo.url, connectionInfo.protocols);
                }
            }, delay);
        } else {
            console.error(`WebSocket重连失败，已达到最大重试次数: ${name}`);
            this.emit(name, 'reconnect_failed', { attempts });
        }
    }

    // 发送消息
    send(name, data) {
        const connectionInfo = this.connections.get(name);
        
        if (connectionInfo && connectionInfo.ws.readyState === WebSocket.OPEN) {
            const message = typeof data === 'string' ? data : JSON.stringify(data);
            connectionInfo.ws.send(message);
            return true;
        }
        
        console.warn(`无法发送消息，WebSocket连接不可用: ${name}`);
        return false;
    }

    // 添加事件监听器
    on(name, event, callback) {
        const key = `${name}:${event}`;
        
        if (!this.listeners.has(key)) {
            this.listeners.set(key, new Set());
        }
        
        this.listeners.get(key).add(callback);
    }

    // 移除事件监听器
    off(name, event, callback) {
        const key = `${name}:${event}`;
        const callbacks = this.listeners.get(key);
        
        if (callbacks) {
            callbacks.delete(callback);
            if (callbacks.size === 0) {
                this.listeners.delete(key);
            }
        }
    }

    // 触发事件
    emit(name, event, data) {
        const key = `${name}:${event}`;
        const callbacks = this.listeners.get(key);
        
        if (callbacks) {
            callbacks.forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error('WebSocket事件处理器错误:', error);
                }
            });
        }
    }

    // 断开连接
    disconnect(name) {
        const connectionInfo = this.connections.get(name);
        
        if (connectionInfo) {
            connectionInfo.ws.close(1000, 'Client disconnect');
            this.connections.delete(name);
            this.reconnectAttempts.delete(name);
        }
    }

    // 获取连接状态
    getConnectionStatus(name) {
        const connectionInfo = this.connections.get(name);
        return connectionInfo ? connectionInfo.status : 'disconnected';
    }

    // 更新连接状态
    updateConnectionStatus(name, status) {
        const connectionInfo = this.connections.get(name);
        if (connectionInfo) {
            connectionInfo.status = status;
        }
    }

    // 重置重连尝试次数
    resetReconnectAttempts(name) {
        this.reconnectAttempts.delete(name);
    }

    // 断开所有连接
    disconnectAll() {
        for (const name of this.connections.keys()) {
            this.disconnect(name);
        }
    }
}

// 导出单例
export const wsManager = new WebSocketManager();
```

#### 🎣 WebSocket自定义Hook

```javascript
// hooks/useWebSocket.js
import { useEffect, useRef, useState } from 'react';
import { wsManager } from '../services/websocket/WebSocketManager';

export const useWebSocket = (name, url, options = {}) => {
    const {
        protocols = [],
        onMessage,
        onConnected,
        onDisconnected,
        onError,
        autoConnect = true,
        dependencies = []
    } = options;

    const [connectionStatus, setConnectionStatus] = useState('disconnected');
    const [lastMessage, setLastMessage] = useState(null);
    const [error, setError] = useState(null);
    
    const callbacksRef = useRef({ onMessage, onConnected, onDisconnected, onError });
    
    // 更新回调引用
    useEffect(() => {
        callbacksRef.current = { onMessage, onConnected, onDisconnected, onError };
    });

    // 设置WebSocket事件监听
    useEffect(() => {
        const handleMessage = (data) => {
            setLastMessage(data);
            callbacksRef.current.onMessage?.(data);
        };

        const handleConnected = (event) => {
            setConnectionStatus('connected');
            setError(null);
            callbacksRef.current.onConnected?.(event);
        };

        const handleDisconnected = (event) => {
            setConnectionStatus('disconnected');
            callbacksRef.current.onDisconnected?.(event);
        };

        const handleError = (error) => {
            setConnectionStatus('error');
            setError(error);
            callbacksRef.current.onError?.(error);
        };

        // 注册事件监听器
        wsManager.on(name, 'message', handleMessage);
        wsManager.on(name, 'connected', handleConnected);
        wsManager.on(name, 'disconnected', handleDisconnected);
        wsManager.on(name, 'error', handleError);

        return () => {
            // 清理事件监听器
            wsManager.off(name, 'message', handleMessage);
            wsManager.off(name, 'connected', handleConnected);
            wsManager.off(name, 'disconnected', handleDisconnected);
            wsManager.off(name, 'error', handleError);
        };
    }, [name]);

    // 自动连接
    useEffect(() => {
        if (autoConnect && url) {
            wsManager.connect(name, url, protocols);
            setConnectionStatus('connecting');
        }

        return () => {
            if (autoConnect) {
                wsManager.disconnect(name);
            }
        };
    }, [name, url, autoConnect, ...dependencies]);

    // 手动连接
    const connect = () => {
        if (url) {
            wsManager.connect(name, url, protocols);
            setConnectionStatus('connecting');
        }
    };

    // 断开连接
    const disconnect = () => {
        wsManager.disconnect(name);
    };

    // 发送消息
    const sendMessage = (data) => {
        return wsManager.send(name, data);
    };

    return {
        connectionStatus,
        lastMessage,
        error,
        connect,
        disconnect,
        sendMessage,
        isConnected: connectionStatus === 'connected',
        isConnecting: connectionStatus === 'connecting'
    };
};

// 使用示例
export const useDeviceWebSocket = () => {
    return useWebSocket(
        'deviceMonitor',
        'ws://localhost:8084/ws/device',
        {
            onMessage: (data) => {
                console.log('设备状态更新:', data);
            },
            onConnected: () => {
                console.log('设备监控WebSocket已连接');
            },
            onError: (error) => {
                console.error('设备监控WebSocket错误:', error);
            }
        }
    );
};
```

### 6.4 性能优化方案

#### ⚡ 虚拟滚动实现

```javascript
// components/common/VirtualList/VirtualList.jsx
import React, { useState, useEffect, useMemo, useRef } from 'react';

const VirtualList = ({
    items,
    itemHeight,
    containerHeight,
    renderItem,
    overscan = 5,
    className = ''
}) => {
    const [scrollTop, setScrollTop] = useState(0);
    const containerRef = useRef(null);

    // 计算可见范围
    const visibleRange = useMemo(() => {
        const startIndex = Math.floor(scrollTop / itemHeight);
        const endIndex = Math.min(
            startIndex + Math.ceil(containerHeight / itemHeight),
            items.length
        );

        return {
            start: Math.max(0, startIndex - overscan),
            end: Math.min(items.length, endIndex + overscan)
        };
    }, [scrollTop, itemHeight, containerHeight, items.length, overscan]);

    // 处理滚动
    const handleScroll = (e) => {
        setScrollTop(e.target.scrollTop);
    };

    // 渲染可见项目
    const visibleItems = useMemo(() => {
        const result = [];
        
        for (let i = visibleRange.start; i < visibleRange.end; i++) {
            const item = items[i];
            const top = i * itemHeight;
            
            result.push(
                <div
                    key={item.id || i}
                    style={{
                        position: 'absolute',
                        top,
                        left: 0,
                        right: 0,
                        height: itemHeight
                    }}
                >
                    {renderItem(item, i)}
                </div>
            );
        }
        
        return result;
    }, [items, visibleRange, itemHeight, renderItem]);

    const totalHeight = items.length * itemHeight;

    return (
        <div
            ref={containerRef}
            className={`virtual-list ${className}`}
            style={{
                height: containerHeight,
                overflow: 'auto'
            }}
            onScroll={handleScroll}
        >
            <div
                style={{
                    position: 'relative',
                    height: totalHeight
                }}
            >
                {visibleItems}
            </div>
        </div>
    );
};

export default VirtualList;

// 使用示例 - 设备列表
const DeviceList = ({ devices }) => {
    const renderDeviceItem = (device, index) => (
        <div className="device-item">
            <div className="device-info">
                <span className={`status-dot ${device.status}`}></span>
                <span className="device-name">{device.name}</span>
                <span className="device-location">{device.location}</span>
            </div>
            <div className="device-actions">
                <button onClick={() => controlDevice(device.id)}>控制</button>
            </div>
        </div>
    );

    return (
        <VirtualList
            items={devices}
            itemHeight={60}
            containerHeight={600}
            renderItem={renderDeviceItem}
            className="device-virtual-list"
        />
    );
};
```

#### 🎯 智能缓存策略

```javascript
// services/cache/SmartCache.js
class SmartCache {
    constructor(options = {}) {
        this.maxSize = options.maxSize || 100;
        this.defaultTTL = options.defaultTTL || 5 * 60 * 1000; // 5分钟
        this.cache = new Map();
        this.timers = new Map();
        this.accessCount = new Map();
        this.lastAccess = new Map();
    }

    // 设置缓存
    set(key, value, ttl = this.defaultTTL) {
        // 如果缓存已满，清理最不常用的项目
        if (this.cache.size >= this.maxSize && !this.cache.has(key)) {
            this.evictLeastUsed();
        }

        // 清理现有的定时器
        if (this.timers.has(key)) {
            clearTimeout(this.timers.get(key));
        }

        // 设置新值
        this.cache.set(key, value);
        this.accessCount.set(key, 0);
        this.lastAccess.set(key, Date.now());

        // 设置过期定时器
        if (ttl > 0) {
            const timer = setTimeout(() => {
                this.delete(key);
            }, ttl);
            this.timers.set(key, timer);
        }
    }

    // 获取缓存
    get(key) {
        if (this.cache.has(key)) {
            // 更新访问统计
            this.accessCount.set(key, (this.accessCount.get(key) || 0) + 1);
            this.lastAccess.set(key, Date.now());
            return this.cache.get(key);
        }
        return null;
    }

    // 删除缓存
    delete(key) {
        if (this.timers.has(key)) {
            clearTimeout(this.timers.get(key));
            this.timers.delete(key);
        }
        
        this.cache.delete(key);
        this.accessCount.delete(key);
        this.lastAccess.delete(key);
    }

    // 清除最不常用的项目
    evictLeastUsed() {
        let leastUsedKey = null;
        let minScore = Infinity;

        for (const [key] of this.cache) {
            const accessCount = this.accessCount.get(key) || 0;
            const lastAccess = this.lastAccess.get(key) || 0;
            const timeSinceAccess = Date.now() - lastAccess;
            
            // 综合访问频率和最近访问时间计算分数
            const score = accessCount / (timeSinceAccess + 1);
            
            if (score < minScore) {
                minScore = score;
                leastUsedKey = key;
            }
        }

        if (leastUsedKey) {
            this.delete(leastUsedKey);
        }
    }

    // 清空缓存
    clear() {
        for (const timer of this.timers.values()) {
            clearTimeout(timer);
        }
        
        this.cache.clear();
        this.timers.clear();
        this.accessCount.clear();
        this.lastAccess.clear();
    }

    // 获取缓存统计
    getStats() {
        return {
            size: this.cache.size,
            maxSize: this.maxSize,
            hitRate: this.calculateHitRate(),
            items: Array.from(this.cache.keys()).map(key => ({
                key,
                accessCount: this.accessCount.get(key),
                lastAccess: this.lastAccess.get(key)
            }))
        };
    }
}

// 创建不同用途的缓存实例
export const deviceCache = new SmartCache({ maxSize: 1000, defaultTTL: 30000 }); // 30秒
export const userCache = new SmartCache({ maxSize: 500, defaultTTL: 300000 }); // 5分钟
export const fileCache = new SmartCache({ maxSize: 200, defaultTTL: 600000 }); // 10分钟
```

---

## 📝 Sequential Thinking Phase 7: 开发指南与最佳实践

### 7.1 组件开发规范

#### 🧩 组件分类与命名

```javascript
// 1. 页面组件 (Page Components) - 大驼峰 + Page后缀
const DashboardPage = () => { /* ... */ };
const UserManagementPage = () => { /* ... */ };

// 2. 布局组件 (Layout Components) - 大驼峰 + Layout后缀
const MainLayout = () => { /* ... */ };
const SidebarLayout = () => { /* ... */ };

// 3. 业务组件 (Business Components) - 大驼峰，体现业务含义
const DeviceStatusCard = () => { /* ... */ };
const UserRoleSelector = () => { /* ... */ };
const FileUploadProgress = () => { /* ... */ };

// 4. 通用组件 (Common Components) - 大驼峰，不带业务含义
const Button = () => { /* ... */ };
const Modal = () => { /* ... */ };
const DataTable = () => { /* ... */ };

// 5. Hook组件 - use前缀 + 小驼峰
const useDeviceStatus = () => { /* ... */ };
const useFileUpload = () => { /* ... */ };
const useRealTimeData = () => { /* ... */ };
```

#### 📋 组件模板规范

```javascript
// components/business/DeviceStatusCard/DeviceStatusCard.jsx
import React, { memo, useCallback } from 'react';
import PropTypes from 'prop-types';
import { Card, Badge, Button, Tooltip } from 'antd';
import { useDeviceStatus } from '../../../hooks/useDeviceStatus';
import './DeviceStatusCard.less';

/**
 * 设备状态卡片组件
 * 
 * @component
 * @example
 * <DeviceStatusCard 
 *   device={deviceData} 
 *   onControl={handleControl}
 *   showActions={true}
 * />
 */
const DeviceStatusCard = memo(({
    device,
    onControl,
    onViewDetails,
    showActions = true,
    className = '',
    ...rest
}) => {
    // 使用自定义Hook获取设备状态
    const { status, isOnline, lastHeartbeat } = useDeviceStatus(device.id);

    // 处理设备控制
    const handleControl = useCallback((action) => {
        onControl?.(device.id, action);
    }, [device.id, onControl]);

    // 处理查看详情
    const handleViewDetails = useCallback(() => {
        onViewDetails?.(device);
    }, [device, onViewDetails]);

    // 获取状态颜色
    const getStatusColor = (status) => {
        const colors = {
            online: '#52c41a',
            offline: '#f5222d',
            standby: '#faad14',
            error: '#ff4d4f'
        };
        return colors[status] || '#d9d9d9';
    };

    // 渲染设备操作按钮
    const renderActions = () => {
        if (!showActions) return null;

        return (
            <div className="device-actions">
                <Button 
                    type="primary" 
                    size="small"
                    onClick={() => handleControl('restart')}
                    disabled={!isOnline}
                >
                    重启
                </Button>
                <Button 
                    size="small"
                    onClick={handleViewDetails}
                >
                    详情
                </Button>
            </div>
        );
    };

    return (
        <Card
            className={`device-status-card ${className}`}
            size="small"
            hoverable
            {...rest}
        >
            <div className="device-header">
                <div className="device-info">
                    <Badge 
                        color={getStatusColor(status)}
                        text={device.name}
                        className="device-name"
                    />
                    <div className="device-location">{device.location}</div>
                </div>
                <div className="device-status">
                    <Tooltip title={`最后心跳: ${lastHeartbeat}`}>
                        <div className={`status-indicator ${status}`} />
                    </Tooltip>
                </div>
            </div>
            
            <div className="device-details">
                <div className="detail-item">
                    <span className="label">型号:</span>
                    <span className="value">{device.model}</span>
                </div>
                <div className="detail-item">
                    <span className="label">分辨率:</span>
                    <span className="value">{device.resolution}</span>
                </div>
            </div>

            {renderActions()}
        </Card>
    );
});

// 组件显示名称
DeviceStatusCard.displayName = 'DeviceStatusCard';

// 属性类型定义
DeviceStatusCard.propTypes = {
    /** 设备信息对象 */
    device: PropTypes.shape({
        id: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        location: PropTypes.string,
        model: PropTypes.string,
        resolution: PropTypes.string
    }).isRequired,
    /** 设备控制回调函数 */
    onControl: PropTypes.func,
    /** 查看详情回调函数 */
    onViewDetails: PropTypes.func,
    /** 是否显示操作按钮 */
    showActions: PropTypes.bool,
    /** 自定义CSS类名 */
    className: PropTypes.string
};

export default DeviceStatusCard;
```

### 7.2 状态管理最佳实践

#### 🗄️ Redux Store结构

```javascript
// store/index.js
import { configureStore } from '@reduxjs/toolkit';
import { authSlice } from './slices/authSlice';
import { deviceSlice } from './slices/deviceSlice';
import { fileSlice } from './slices/fileSlice';
import { messageSlice } from './slices/messageSlice';
import { uiSlice } from './slices/uiSlice';
import { apiMiddleware } from './middleware/apiMiddleware';
import { websocketMiddleware } from './middleware/websocketMiddleware';

export const store = configureStore({
    reducer: {
        auth: authSlice.reducer,
        device: deviceSlice.reducer,
        file: fileSlice.reducer,
        message: messageSlice.reducer,
        ui: uiSlice.reducer,
    },
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
            serializableCheck: {
                ignoredActions: ['websocket/messageReceived'],
                ignoredPaths: ['websocket.connection']
            }
        }).concat(
            apiMiddleware,
            websocketMiddleware
        ),
    devTools: process.env.NODE_ENV !== 'production'
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

#### 🎛️ Slice示例 - 设备管理

```javascript
// store/slices/deviceSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { apiClient } from '../../services/api/client';

// 异步Thunk - 获取设备列表
export const fetchDevices = createAsyncThunk(
    'device/fetchDevices',
    async (params, { rejectWithValue }) => {
        try {
            const response = await apiClient.device.list(params);
            return response;
        } catch (error) {
            return rejectWithValue(error.message);
        }
    }
);

// 异步Thunk - 创建设备
export const createDevice = createAsyncThunk(
    'device/createDevice',
    async (deviceData, { rejectWithValue }) => {
        try {
            const response = await apiClient.device.create(deviceData);
            return response;
        } catch (error) {
            return rejectWithValue(error.message);
        }
    }
);

// 异步Thunk - 获取设备组树
export const fetchDeviceGroups = createAsyncThunk(
    'device/fetchDeviceGroups',
    async (_, { rejectWithValue }) => {
        try {
            const response = await apiClient.device.getGroups();
            return response;
        } catch (error) {
            return rejectWithValue(error.message);
        }
    }
);

const initialState = {
    // 设备列表
    devices: [],
    devicesLoading: false,
    devicesError: null,
    
    // 设备组
    deviceGroups: [],
    groupsLoading: false,
    groupsError: null,
    
    // 当前选中的设备
    selectedDevice: null,
    
    // 设备实时状态
    deviceStatus: {},
    
    // 过滤和搜索
    filters: {
        status: 'all',
        group: null,
        keyword: ''
    },
    
    // 分页信息
    pagination: {
        current: 1,
        pageSize: 20,
        total: 0
    }
};

const deviceSlice = createSlice({
    name: 'device',
    initialState,
    reducers: {
        // 设置选中的设备
        setSelectedDevice: (state, action) => {
            state.selectedDevice = action.payload;
        },
        
        // 更新设备实时状态
        updateDeviceStatus: (state, action) => {
            const { deviceId, status } = action.payload;
            state.deviceStatus[deviceId] = {
                ...state.deviceStatus[deviceId],
                ...status,
                lastUpdate: Date.now()
            };
            
            // 同时更新设备列表中的状态
            const deviceIndex = state.devices.findIndex(d => d.id === deviceId);
            if (deviceIndex !== -1) {
                state.devices[deviceIndex] = {
                    ...state.devices[deviceIndex],
                    ...status
                };
            }
        },
        
        // 设置过滤条件
        setFilters: (state, action) => {
            state.filters = { ...state.filters, ...action.payload };
        },
        
        // 设置分页信息
        setPagination: (state, action) => {
            state.pagination = { ...state.pagination, ...action.payload };
        },
        
        // 清空错误状态
        clearError: (state) => {
            state.devicesError = null;
            state.groupsError = null;
        }
    },
    extraReducers: (builder) => {
        builder
            // 获取设备列表
            .addCase(fetchDevices.pending, (state) => {
                state.devicesLoading = true;
                state.devicesError = null;
            })
            .addCase(fetchDevices.fulfilled, (state, action) => {
                state.devicesLoading = false;
                state.devices = action.payload.items || [];
                state.pagination.total = action.payload.total || 0;
            })
            .addCase(fetchDevices.rejected, (state, action) => {
                state.devicesLoading = false;
                state.devicesError = action.payload;
            })
            
            // 创建设备
            .addCase(createDevice.pending, (state) => {
                state.devicesError = null;
            })
            .addCase(createDevice.fulfilled, (state, action) => {
                state.devices.unshift(action.payload);
                state.pagination.total += 1;
            })
            .addCase(createDevice.rejected, (state, action) => {
                state.devicesError = action.payload;
            })
            
            // 获取设备组
            .addCase(fetchDeviceGroups.pending, (state) => {
                state.groupsLoading = true;
                state.groupsError = null;
            })
            .addCase(fetchDeviceGroups.fulfilled, (state, action) => {
                state.groupsLoading = false;
                state.deviceGroups = action.payload;
            })
            .addCase(fetchDeviceGroups.rejected, (state, action) => {
                state.groupsLoading = false;
                state.groupsError = action.payload;
            });
    }
});

export const {
    setSelectedDevice,
    updateDeviceStatus,
    setFilters,
    setPagination,
    clearError
} = deviceSlice.actions;

export { deviceSlice };
```

### 7.3 代码质量保证

#### 🧪 测试策略

```javascript
// __tests__/components/DeviceStatusCard.test.jsx
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import DeviceStatusCard from '../components/business/DeviceStatusCard/DeviceStatusCard';
import { deviceSlice } from '../store/slices/deviceSlice';

// 创建测试用的store
const createTestStore = (initialState = {}) => {
    return configureStore({
        reducer: {
            device: deviceSlice.reducer
        },
        preloadedState: initialState
    });
};

// 测试用的设备数据
const mockDevice = {
    id: 'device-001',
    name: 'LED Display 01',
    location: '北京-办公楼A',
    model: 'P4室内全彩',
    resolution: '1920x1080',
    status: 'online'
};

describe('DeviceStatusCard', () => {
    let mockOnControl;
    let mockOnViewDetails;
    
    beforeEach(() => {
        mockOnControl = jest.fn();
        mockOnViewDetails = jest.fn();
    });

    it('应该正确渲染设备信息', () => {
        const store = createTestStore();
        
        render(
            <Provider store={store}>
                <DeviceStatusCard 
                    device={mockDevice}
                    onControl={mockOnControl}
                    onViewDetails={mockOnViewDetails}
                />
            </Provider>
        );

        expect(screen.getByText('LED Display 01')).toBeInTheDocument();
        expect(screen.getByText('北京-办公楼A')).toBeInTheDocument();
        expect(screen.getByText('P4室内全彩')).toBeInTheDocument();
    });

    it('应该在设备在线时显示重启按钮', () => {
        const store = createTestStore();
        
        render(
            <Provider store={store}>
                <DeviceStatusCard 
                    device={mockDevice}
                    onControl={mockOnControl}
                    showActions={true}
                />
            </Provider>
        );

        const restartButton = screen.getByText('重启');
        expect(restartButton).toBeInTheDocument();
        expect(restartButton).not.toBeDisabled();
    });

    it('应该在设备离线时禁用重启按钮', () => {
        const offlineDevice = { ...mockDevice, status: 'offline' };
        const store = createTestStore();
        
        render(
            <Provider store={store}>
                <DeviceStatusCard 
                    device={offlineDevice}
                    onControl={mockOnControl}
                    showActions={true}
                />
            </Provider>
        );

        const restartButton = screen.getByText('重启');
        expect(restartButton).toBeDisabled();
    });

    it('应该在点击重启按钮时调用onControl回调', () => {
        const store = createTestStore();
        
        render(
            <Provider store={store}>
                <DeviceStatusCard 
                    device={mockDevice}
                    onControl={mockOnControl}
                    showActions={true}
                />
            </Provider>
        );

        fireEvent.click(screen.getByText('重启'));
        expect(mockOnControl).toHaveBeenCalledWith('device-001', 'restart');
    });

    it('应该在点击详情按钮时调用onViewDetails回调', () => {
        const store = createTestStore();
        
        render(
            <Provider store={store}>
                <DeviceStatusCard 
                    device={mockDevice}
                    onViewDetails={mockOnViewDetails}
                    showActions={true}
                />
            </Provider>
        );

        fireEvent.click(screen.getByText('详情'));
        expect(mockOnViewDetails).toHaveBeenCalledWith(mockDevice);
    });
});
```

#### 📊 性能监控

```javascript
// utils/performance.js
class PerformanceMonitor {
    constructor() {
        this.metrics = new Map();
        this.observers = [];
        this.init();
    }

    init() {
        // 监控页面加载性能
        if ('performance' in window && 'PerformanceObserver' in window) {
            this.observeNavigationTiming();
            this.observeLCP();
            this.observeFID();
            this.observeCLS();
        }
    }

    // 监控导航时间
    observeNavigationTiming() {
        window.addEventListener('load', () => {
            const navTiming = performance.getEntriesByType('navigation')[0];
            
            this.recordMetric('page_load_time', navTiming.loadEventEnd - navTiming.fetchStart);
            this.recordMetric('dom_content_loaded', navTiming.domContentLoadedEventEnd - navTiming.fetchStart);
            this.recordMetric('first_byte', navTiming.responseStart - navTiming.fetchStart);
        });
    }

    // 监控最大内容绘制(LCP)
    observeLCP() {
        const observer = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                this.recordMetric('largest_contentful_paint', entry.startTime);
            }
        });
        
        observer.observe({ entryTypes: ['largest-contentful-paint'] });
        this.observers.push(observer);
    }

    // 监控首次输入延迟(FID)
    observeFID() {
        const observer = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                this.recordMetric('first_input_delay', entry.processingStart - entry.startTime);
            }
        });
        
        observer.observe({ entryTypes: ['first-input'] });
        this.observers.push(observer);
    }

    // 监控累积布局偏移(CLS)
    observeCLS() {
        let clsValue = 0;
        let clsEntries = [];
        
        const observer = new PerformanceObserver((list) => {
            for (const entry of list.getEntries()) {
                if (!entry.hadRecentInput) {
                    clsValue += entry.value;
                    clsEntries.push(entry);
                }
            }
            
            this.recordMetric('cumulative_layout_shift', clsValue);
        });
        
        observer.observe({ entryTypes: ['layout-shift'] });
        this.observers.push(observer);
    }

    // 记录自定义指标
    recordMetric(name, value, tags = {}) {
        const metric = {
            name,
            value,
            timestamp: Date.now(),
            tags: {
                url: window.location.pathname,
                userAgent: navigator.userAgent,
                ...tags
            }
        };

        this.metrics.set(`${name}_${Date.now()}`, metric);
        
        // 发送到监控服务
        this.sendMetric(metric);
    }

    // 开始性能测量
    startMeasure(name) {
        performance.mark(`${name}-start`);
    }

    // 结束性能测量
    endMeasure(name) {
        performance.mark(`${name}-end`);
        performance.measure(name, `${name}-start`, `${name}-end`);
        
        const measure = performance.getEntriesByName(name, 'measure')[0];
        this.recordMetric(name, measure.duration);
        
        // 清理标记
        performance.clearMarks(`${name}-start`);
        performance.clearMarks(`${name}-end`);
        performance.clearMeasures(name);
    }

    // 发送指标到监控服务
    sendMetric(metric) {
        // 批量发送，避免影响性能
        if (!this.sendQueue) {
            this.sendQueue = [];
            setTimeout(() => this.flushMetrics(), 1000);
        }
        
        this.sendQueue.push(metric);
    }

    // 批量发送指标
    flushMetrics() {
        if (this.sendQueue && this.sendQueue.length > 0) {
            fetch('/api/metrics', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.sendQueue)
            }).catch(error => {
                console.warn('发送性能指标失败:', error);
            });
            
            this.sendQueue = null;
        }
    }

    // 获取性能报告
    getPerformanceReport() {
        const metrics = Array.from(this.metrics.values());
        
        return {
            pageLoad: metrics.filter(m => m.name === 'page_load_time'),
            webVitals: {
                lcp: metrics.filter(m => m.name === 'largest_contentful_paint'),
                fid: metrics.filter(m => m.name === 'first_input_delay'),
                cls: metrics.filter(m => m.name === 'cumulative_layout_shift')
            },
            custom: metrics.filter(m => !['page_load_time', 'largest_contentful_paint', 'first_input_delay', 'cumulative_layout_shift'].includes(m.name))
        };
    }

    // 清理资源
    destroy() {
        this.observers.forEach(observer => observer.disconnect());
        this.observers = [];
        this.metrics.clear();
    }
}

// 创建全局性能监控实例
export const performanceMonitor = new PerformanceMonitor();

// React组件性能监控Hook
export const usePerformanceMonitor = (componentName) => {
    const startTime = useRef(Date.now());
    
    useEffect(() => {
        performanceMonitor.startMeasure(`component_${componentName}`);
        
        return () => {
            performanceMonitor.endMeasure(`component_${componentName}`);
        };
    }, [componentName]);
    
    const recordCustomMetric = useCallback((metricName, value, tags = {}) => {
        performanceMonitor.recordMetric(
            `${componentName}_${metricName}`, 
            value, 
            { component: componentName, ...tags }
        );
    }, [componentName]);
    
    return { recordCustomMetric };
};
```

---

## 🎯 结论与实施建议

### 实施优先级

#### 🚀 第一阶段 (MVP - 4周)
1. **基础框架搭建** - 完成项目架构和基础组件
2. **用户认证系统** - 实现OAuth2登录和权限控制
3. **主控制台** - 核心仪表板和导航
4. **设备管理基础功能** - 设备列表、状态监控
5. **响应式适配** - 确保移动端基本可用

#### ⚡ 第二阶段 (完整功能 - 6周)
1. **实时通信** - WebSocket集成和实时数据更新
2. **文件管理** - 文件上传、转码进度监控
3. **消息中心** - 实时消息推送和通知
4. **高级设备管理** - 批量操作、详细控制
5. **权限精细化** - 基于Casbin的细粒度权限控制

#### 🔧 第三阶段 (优化提升 - 4周)
1. **性能优化** - 虚拟滚动、智能缓存
2. **用户体验提升** - 动画效果、交互优化
3. **监控和分析** - 性能监控、用户行为分析
4. **国际化支持** - 多语言切换
5. **PWA支持** - 离线访问、推送通知

### 技术风险和应对方案

#### ⚠️ 主要技术风险
1. **WebSocket连接稳定性** - 使用连接池和自动重连机制
2. **大数据量渲染性能** - 虚拟滚动和分页策略
3. **实时数据同步复杂性** - 状态管理规范化和冲突处理
4. **权限控制复杂性** - 基于Casbin的标准化权限模型

#### 🛡️ 质量保证措施
1. **代码规范** - ESLint + Prettier + Husky
2. **类型安全** - TypeScript全覆盖
3. **测试覆盖** - 单元测试 + 集成测试 + E2E测试
4. **性能监控** - 实时性能指标收集和分析

---

> **文档版本**: v1.0  
> **创建时间**: 2024-07-26  
> **最后更新**: 2024-07-26  
> **维护团队**: LedDeviceCloudPlatform Frontend Team

本设计文档基于Sequential Thinking方法论，从用户需求出发，逐步构建了完整的前端UI/UX解决方案。文档涵盖了从用户分析到技术实现的全过程，为前端开发团队提供了详细的设计指导和实施路线图。

随着项目的发展和用户反馈，本文档将持续更新和完善，确保设计方案与实际需求保持一致。