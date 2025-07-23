#!/bin/bash

#########################################################################
# LED设备终端通信服务 - Linux系统参数调优脚本
# 
# 用途：针对10k并发WebSocket连接进行系统级性能调优
# 目标：支持10000个并发连接，预留扩展到20000个连接的能力
# 
# 使用方法：
# 1. sudo chmod +x linux-system-tuning.sh
# 2. sudo ./linux-system-tuning.sh
# 3. 重启系统或执行 sudo sysctl -p 使配置生效
#########################################################################

echo "[INFO] 开始配置LED设备终端通信服务系统参数优化..."

# 备份原有配置
BACKUP_DIR="/etc/sysctl.d/backup"
BACKUP_FILE="$BACKUP_DIR/original-sysctl-$(date +%Y%m%d-%H%M%S).conf"

if [ ! -d "$BACKUP_DIR" ]; then
    sudo mkdir -p "$BACKUP_DIR"
fi

echo "[INFO] 备份原有系统配置到: $BACKUP_FILE"
sudo sysctl -a > "$BACKUP_FILE" 2>/dev/null

# 创建LED设备终端服务专用的系统参数配置文件
SYSCTL_CONF="/etc/sysctl.d/99-terminal-service.conf"

echo "[INFO] 创建系统参数配置文件: $SYSCTL_CONF"

sudo tee "$SYSCTL_CONF" > /dev/null << 'EOF'
#########################################################################
# LED设备终端通信服务 - 系统参数优化配置
# 针对10k并发WebSocket连接优化，预留20k扩展能力
#########################################################################

# ================================
# 网络连接参数优化
# ================================

# TCP连接队列长度 - 支持高并发连接建立
# 默认值通常为128，提升到32768支持大量并发连接
net.core.somaxconn = 32768

# 网络设备队列长度 - 提升网络包处理能力
# 默认值通常为1000，提升到10000
net.core.netdev_max_backlog = 10000

# TCP SYN队列长度 - 支持更多的TCP连接建立
# 默认值通常为1024，提升到8192
net.ipv4.tcp_max_syn_backlog = 8192

# TCP连接重用和回收优化
# 启用TIME_WAIT套接字重用，加快连接回收
net.ipv4.tcp_tw_reuse = 1

# 减少TIME_WAIT状态超时时间(秒)
# 默认60秒，减少到30秒加快端口回收
net.ipv4.tcp_fin_timeout = 30

# TCP保持连接参数优化
# TCP keepalive探测间隔(秒) - 检测断开的连接
net.ipv4.tcp_keepalive_time = 600
# TCP keepalive探测次数
net.ipv4.tcp_keepalive_probes = 3
# TCP keepalive探测间隔(秒)
net.ipv4.tcp_keepalive_intvl = 15

# ================================
# 文件描述符限制优化
# ================================

# 系统级最大文件描述符数量
# 支持10k连接 + 数据库连接 + 其他文件句柄
# 预留20k扩展能力，设置为65535
fs.file-max = 65535

# 单个进程最大文件描述符数量
# 通过ulimit在启动脚本中设置

# ================================
# 内存管理参数优化
# ================================

# 虚拟内存管理优化
# 控制内存回收的激进程度，0-100，值越小越不激进
# 默认60，设置为10减少不必要的内存回收
vm.swappiness = 10

# 脏页回写参数优化
# 脏页占总内存的百分比阈值，触发后台回写
vm.dirty_background_ratio = 5
# 脏页占总内存的百分比阈值，触发同步回写
vm.dirty_ratio = 10

# ================================
# TCP缓冲区优化
# ================================

# TCP接收缓冲区大小调优 (最小值 默认值 最大值)
# 优化WebSocket数据接收性能
net.ipv4.tcp_rmem = 8192 87380 16777216

# TCP发送缓冲区大小调优 (最小值 默认值 最大值)
# 优化WebSocket数据发送性能
net.ipv4.tcp_wmem = 8192 65536 16777216

# 核心网络缓冲区大小
# 接收缓冲区最大值
net.core.rmem_max = 16777216
# 发送缓冲区最大值
net.core.wmem_max = 16777216
# 默认接收缓冲区大小
net.core.rmem_default = 262144
# 默认发送缓冲区大小
net.core.wmem_default = 262144

# ================================
# TCP协议栈优化
# ================================

# 启用TCP窗口缩放，支持高带宽长延迟网络
net.ipv4.tcp_window_scaling = 1

# 启用TCP选择性确认，提升重传效率
net.ipv4.tcp_sack = 1

# 启用TCP转发确认，提升吞吐量
net.ipv4.tcp_fack = 1

# TCP拥塞控制算法 - 使用BBR提升性能
# 如果内核支持BBR，使用BBR；否则使用cubic
net.core.default_qdisc = fq
net.ipv4.tcp_congestion_control = bbr

# ================================
# 安全相关参数
# ================================

# 限制SYN flood攻击
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_max_tw_buckets = 20000

# 限制核心转储文件大小
kernel.core_pattern = /tmp/core-%e-%p-%t

# ================================
# 其他性能优化参数
# ================================

# 进程可创建的最大内存映射区域数量
vm.max_map_count = 262144

# 允许的最大进程数
kernel.pid_max = 4194304

# 共享内存段最大值(字节)
kernel.shmmax = 2147483648

# 系统级信号量限制
kernel.sem = 5010 641280 5010 128

EOF

echo "[INFO] 系统参数配置文件创建完成"

# 创建用户级文件描述符限制配置
LIMITS_CONF="/etc/security/limits.d/99-terminal-service.conf"

echo "[INFO] 创建用户限制配置文件: $LIMITS_CONF"

sudo tee "$LIMITS_CONF" > /dev/null << 'EOF'
# LED设备终端通信服务 - 用户资源限制配置
# 支持高并发连接和文件操作

# 软限制和硬限制设置格式: <domain> <type> <item> <value>
# domain: 用户名或组名，* 表示所有用户
# type: soft(软限制) 或 hard(硬限制)
# item: 资源类型
# value: 限制值

# 文件描述符限制 - 支持10k+连接
*               soft    nofile          65535
*               hard    nofile          65535
root            soft    nofile          65535
root            hard    nofile          65535

# 进程数限制
*               soft    nproc           32768
*               hard    nproc           32768
root            soft    nproc           32768
root            hard    nproc           32768

# 内存锁定限制(字节) - 支持大内存应用
*               soft    memlock         unlimited
*               hard    memlock         unlimited

# 核心转储文件大小限制
*               soft    core            0
*               hard    core            0

# 栈大小限制(KB)
*               soft    stack           8192
*               hard    stack           8192

EOF

echo "[INFO] 用户限制配置文件创建完成"

# 创建systemd服务级别的资源限制配置
SYSTEMD_CONF="/etc/systemd/system.conf.d/terminal-service.conf"
sudo mkdir -p "/etc/systemd/system.conf.d"

echo "[INFO] 创建systemd服务配置文件: $SYSTEMD_CONF"

sudo tee "$SYSTEMD_CONF" > /dev/null << 'EOF'
[Manager]
# systemd服务级别的资源限制配置
# 适用于LED设备终端通信服务

# 默认文件描述符限制
DefaultLimitNOFILE=65535:65535

# 默认进程数限制
DefaultLimitNPROC=32768:32768

# 默认内存锁定限制
DefaultLimitMEMLOCK=infinity

# 默认栈大小限制
DefaultLimitSTACK=8192:8192

EOF

echo "[INFO] systemd服务配置文件创建完成"

# 应用系统参数配置
echo "[INFO] 应用系统参数配置..."
sudo sysctl -p "$SYSCTL_CONF"

# 验证关键参数是否生效
echo "[INFO] 验证关键系统参数配置..."

check_param() {
    local param=$1
    local expected=$2
    local actual=$(sysctl -n $param 2>/dev/null)
    
    if [ "$actual" = "$expected" ]; then
        echo "[OK] $param = $actual"
    else
        echo "[WARN] $param = $actual (期望: $expected)"
    fi
}

check_param "net.core.somaxconn" "32768"
check_param "fs.file-max" "65535"
check_param "net.ipv4.tcp_max_syn_backlog" "8192"
check_param "vm.swappiness" "10"

# 显示当前文件描述符限制
echo "[INFO] 当前用户文件描述符限制:"
ulimit -n

echo "[INFO] 系统参数优化配置完成！"
echo ""
echo "重要提示："
echo "1. 配置已生效，但建议重启系统确保所有参数完全生效"
echo "2. 可使用 'sudo sysctl -p' 重新加载系统参数"
echo "3. 可使用 'ulimit -n' 检查当前用户文件描述符限制"
echo "4. Java应用启动时需要配置对应的JVM参数"
echo ""
echo "下一步："
echo "请配置Java应用的JVM启动参数以配合系统优化"
echo ""

# 显示建议的JVM启动参数
cat << 'EOF'
建议的JVM启动参数（用于10k并发WebSocket连接）：

-server
-Xms8g
-Xmx16g
-XX:+UseG1GC
-XX:G1HeapRegionSize=16m
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
-XX:NewRatio=2
-XX:SurvivorRatio=8
-XX:MaxTenuringThreshold=15
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCApplicationStoppedTime
-Xloggc:logs/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=100M
-Djava.awt.headless=true
-Dfile.encoding=UTF-8
-Djava.security.egd=file:/dev/./urandom
-Dio.netty.allocator.type=pooled
-Dio.netty.allocator.numDirectArenas=16
-Dio.netty.allocator.numHeapArenas=16
-Dio.netty.allocator.pageSize=8192
-Dio.netty.allocator.maxOrder=11
-Dio.netty.allocator.chunkSize=16777216
-Dio.netty.allocator.smallCacheSize=256
-Dio.netty.allocator.normalCacheSize=64
-Dio.netty.allocator.maxCachedBufferCapacity=32768
-Dio.netty.allocator.cacheTrimInterval=8192
-Dio.netty.leakDetection.level=DISABLED
-Dio.netty.noUnsafe=false
-Dio.netty.noKeySetOptimization=false
-Dio.netty.recycler.maxCapacity.default=4096

EOF

echo "[INFO] 系统调优脚本执行完成！"