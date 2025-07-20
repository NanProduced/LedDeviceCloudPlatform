# Maven依赖下载问题解决方案

## 问题描述
Maven在下载`spring-boot-starter-amqp`依赖时出现网络超时错误。

## 解决方案

### 方案1: 使用国内镜像源（推荐）

已为您配置了多个国内镜像源：
- 华为云镜像（主要）
- 腾讯云镜像（备用）
- 阿里云镜像（备用2）

配置文件位置：`%USERPROFILE%\.m2\settings.xml`

### 方案2: 手动清理缓存并重试

#### Windows环境
```cmd
# 运行清理脚本
fix-maven-cache.bat

# 或手动执行
del /s /q "%USERPROFILE%\.m2\repository\*.lastUpdated"
rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-amqp"
```

#### Linux/Mac环境
```bash
# 运行清理脚本
./fix-maven-cache.sh

# 或手动执行
find ~/.m2/repository -name "*.lastUpdated" -delete
rm -rf ~/.m2/repository/org/springframework/boot/spring-boot-starter-amqp
```

### 方案3: 使用Maven命令强制更新

```bash
# 强制更新依赖
mvn clean compile -U -pl message-service

# 或者使用离线模式（如果依赖已存在）
mvn clean compile -o -pl message-service

# 重新下载所有依赖
mvn dependency:purge-local-repository -pl message-service
mvn clean compile -pl message-service
```

### 方案4: 临时网络代理解决（企业环境）

如果在企业网络环境中，可能需要配置代理：

```xml
<!-- 在 ~/.m2/settings.xml 中添加 -->
<proxies>
  <proxy>
    <id>proxy</id>
    <active>true</active>
    <protocol>http</protocol>
    <host>your-proxy-host</host>
    <port>your-proxy-port</port>
    <username>your-username</username>
    <password>your-password</password>
  </proxy>
</proxies>
```

### 方案5: 手动下载依赖（最后选择）

如果网络问题持续存在，可以：

1. 从另一台可以访问Maven仓库的机器下载依赖
2. 复制整个 `~/.m2/repository` 目录
3. 或使用企业内部Maven仓库

## 常用Maven镜像源

| 镜像源 | URL | 地区 |
|--------|-----|------|
| 华为云 | https://repo.huaweicloud.com/repository/maven/ | 中国 |
| 腾讯云 | https://mirrors.cloud.tencent.com/nexus/repository/maven-public/ | 中国 |
| 阿里云 | https://maven.aliyun.com/repository/central | 中国 |
| 网易云 | https://mirrors.163.com/maven/repository/maven-public/ | 中国 |

## 验证步骤

1. 运行清理脚本
2. 检查网络连接
3. 重新执行Maven构建
4. 如果仍有问题，尝试不同的镜像源

## 备用构建策略

如果message-service模块依赖下载仍有问题，可以：

1. 先构建不依赖RabbitMQ的模块
2. 使用Spring Boot内置的简单消息机制作为临时替代
3. 逐步添加外部依赖

建议优先级：
1. 清理缓存 + 重试
2. 使用华为云镜像
3. 检查网络/代理配置
4. 考虑离线或企业仓库解决方案