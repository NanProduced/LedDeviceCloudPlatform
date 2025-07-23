#!/bin/bash

#########################################################################
# LED设备终端通信服务 - JVM优化启动脚本
# 
# 用途：使用G1GC和高性能参数启动terminal-service
# 目标：支持10k并发WebSocket连接的高性能Java应用
# 
# 使用方法：
# 1. chmod +x jvm-startup.sh
# 2. ./jvm-startup.sh [profile]
#    - 不指定profile默认使用dev环境
#    - 支持的profile: dev, local, prod
#########################################################################

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_FILE="$APP_HOME/target/terminal-boot-1.0.0-SNAPSHOT.jar"
LOG_DIR="$APP_HOME/logs"
PID_FILE="$APP_HOME/terminal-service.pid"

# 默认环境配置
PROFILE=${1:-dev}
APP_NAME="terminal-service"

# 创建日志目录
mkdir -p "$LOG_DIR"

echo "[INFO] LED设备终端通信服务启动脚本"
echo "[INFO] 应用目录: $APP_HOME"
echo "[INFO] 环境配置: $PROFILE"
echo "[INFO] JAR文件: $JAR_FILE"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR文件不存在: $JAR_FILE"
    echo "[INFO] 请先执行: mvn clean package"
    exit 1
fi

# 检查是否已经运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "[WARN] 服务已在运行，PID: $PID"
        echo "[INFO] 如需重启，请先执行: ./jvm-startup.sh stop"
        exit 1
    else
        echo "[INFO] 清理过期的PID文件"
        rm -f "$PID_FILE"
    fi
fi

# ================================
# JVM基础参数配置
# ================================

# 基础JVM参数
JVM_OPTS="$JVM_OPTS -server"
JVM_OPTS="$JVM_OPTS -Djava.awt.headless=true"
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"
JVM_OPTS="$JVM_OPTS -Djava.security.egd=file:/dev/./urandom"

# ================================
# 内存管理参数 - 针对10k并发连接优化
# ================================

case "$PROFILE" in
    "prod")
        # 生产环境 - 16GB内存，支持10k并发连接
        JVM_OPTS="$JVM_OPTS -Xms8g"
        JVM_OPTS="$JVM_OPTS -Xmx16g"
        JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=512m"
        JVM_OPTS="$JVM_OPTS -XX:MaxMetaspaceSize=1g"
        JVM_OPTS="$JVM_OPTS -XX:MaxDirectMemorySize=2g"
        ;;
    "dev")
        # 开发环境 - 4GB内存，支持1k并发连接
        JVM_OPTS="$JVM_OPTS -Xms2g"
        JVM_OPTS="$JVM_OPTS -Xmx4g"
        JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=256m"
        JVM_OPTS="$JVM_OPTS -XX:MaxMetaspaceSize=512m"
        JVM_OPTS="$JVM_OPTS -XX:MaxDirectMemorySize=1g"
        ;;
    "local")
        # 本地环境 - 2GB内存，支持100并发连接
        JVM_OPTS="$JVM_OPTS -Xms1g"
        JVM_OPTS="$JVM_OPTS -Xmx2g"
        JVM_OPTS="$JVM_OPTS -XX:MetaspaceSize=128m"
        JVM_OPTS="$JVM_OPTS -XX:MaxMetaspaceSize=256m"
        JVM_OPTS="$JVM_OPTS -XX:MaxDirectMemorySize=512m"
        ;;
    *)
        echo "[ERROR] 不支持的环境配置: $PROFILE"
        echo "[INFO] 支持的环境: prod, dev, local"
        exit 1
        ;;
esac

# ================================
# G1垃圾回收器优化配置
# ================================

# 启用G1GC垃圾回收器
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"

# G1GC调优参数
JVM_OPTS="$JVM_OPTS -XX:G1HeapRegionSize=16m"          # G1堆区域大小
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"          # 最大GC暂停时间200ms
JVM_OPTS="$JVM_OPTS -XX:G1NewSizePercent=30"           # 新生代最小比例
JVM_OPTS="$JVM_OPTS -XX:G1MaxNewSizePercent=40"        # 新生代最大比例
JVM_OPTS="$JVM_OPTS -XX:G1ReservePercent=15"           # 保留区域百分比
JVM_OPTS="$JVM_OPTS -XX:InitiatingHeapOccupancyPercent=30"  # 并发标记启动阈值

# G1GC优化选项
JVM_OPTS="$JVM_OPTS -XX:+G1UseAdaptiveIHOP"            # 自适应IHOP
JVM_OPTS="$JVM_OPTS -XX:+G1PrintRegionRememberedSetInfo" # G1区域信息打印
JVM_OPTS="$JVM_OPTS -XX:+UnlockExperimentalVMOptions"  # 解锁实验性选项
JVM_OPTS="$JVM_OPTS -XX:G1MixedGCLiveThresholdPercent=85" # 混合GC存活阈值

# ================================
# JIT编译器优化
# ================================

# 编译器优化参数
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"       # 字符串去重
JVM_OPTS="$JVM_OPTS -XX:+OptimizeStringConcat"         # 字符串连接优化
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"            # 压缩对象指针
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedClassPointers"   # 压缩类指针
JVM_OPTS="$JVM_OPTS -XX:+UseFastAccessorMethods"       # 快速访问器方法
JVM_OPTS="$JVM_OPTS -XX:+UseBiasedLocking"             # 偏向锁
JVM_OPTS="$JVM_OPTS -XX:+DoEscapeAnalysis"             # 逃逸分析

# JIT编译阈值调优
JVM_OPTS="$JVM_OPTS -XX:CompileThreshold=1000"         # JIT编译阈值
JVM_OPTS="$JVM_OPTS -XX:Tier3InvocationThreshold=200"  # C1编译器阈值
JVM_OPTS="$JVM_OPTS -XX:Tier4InvocationThreshold=5000" # C2编译器阈值

# ================================
# Netty性能优化参数
# ================================

# Netty内存分配器优化
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.type=pooled"                    # 使用池化分配器
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.numDirectArenas=16"             # 直接内存Arena数量
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.numHeapArenas=16"               # 堆内存Arena数量
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.pageSize=8192"                  # 内存页大小8KB
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.maxOrder=11"                    # 最大分配单元
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.chunkSize=16777216"             # Chunk大小16MB
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.smallCacheSize=256"             # 小对象缓存大小
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.normalCacheSize=64"             # 普通对象缓存大小
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.maxCachedBufferCapacity=32768"  # 最大缓存Buffer容量
JVM_OPTS="$JVM_OPTS -Dio.netty.allocator.cacheTrimInterval=8192"         # 缓存清理间隔

# Netty性能调优参数
JVM_OPTS="$JVM_OPTS -Dio.netty.leakDetection.level=DISABLED"             # 禁用内存泄漏检测
JVM_OPTS="$JVM_OPTS -Dio.netty.noUnsafe=false"                           # 启用Unsafe优化
JVM_OPTS="$JVM_OPTS -Dio.netty.noKeySetOptimization=false"               # 启用KeySet优化
JVM_OPTS="$JVM_OPTS -Dio.netty.recycler.maxCapacity.default=4096"        # 对象回收器容量
JVM_OPTS="$JVM_OPTS -Dio.netty.processLocalThreadCache=true"             # 启用本地线程缓存

# ================================
# 网络和NIO优化参数
# ================================

# NIO优化参数
JVM_OPTS="$JVM_OPTS -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider"
JVM_OPTS="$JVM_OPTS -Dsun.nio.ch.bugLevel=0"
JVM_OPTS="$JVM_OPTS -Dsun.nio.ch.disableSynchronousRead=false"

# 网络相关优化
JVM_OPTS="$JVM_OPTS -Djava.net.preferIPv4Stack=true"                     # 优先使用IPv4
JVM_OPTS="$JVM_OPTS -Djava.net.useSystemProxies=false"                   # 不使用系统代理

# ================================
# GC日志配置
# ================================

GC_LOG_FILE="$LOG_DIR/gc-$PROFILE.log"

# GC日志参数
JVM_OPTS="$JVM_OPTS -Xloggc:$GC_LOG_FILE"
JVM_OPTS="$JVM_OPTS -XX:+PrintGC"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCTimeStamps"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDateStamps"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCApplicationStoppedTime"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCApplicationConcurrentTime"
JVM_OPTS="$JVM_OPTS -XX:+UseGCLogFileRotation"
JVM_OPTS="$JVM_OPTS -XX:NumberOfGCLogFiles=10"
JVM_OPTS="$JVM_OPTS -XX:GCLogFileSize=100M"

# ================================
# 故障诊断和调试参数
# ================================

DUMP_DIR="$LOG_DIR/dumps"
mkdir -p "$DUMP_DIR"

# JVM崩溃时生成堆转储
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=$DUMP_DIR/"

# JVM错误日志
JVM_OPTS="$JVM_OPTS -XX:ErrorFile=$LOG_DIR/hs_err_pid%p.log"

# 飞行记录器配置（生产环境可选）
if [ "$PROFILE" = "prod" ]; then
    JVM_OPTS="$JVM_OPTS -XX:+FlightRecorder"
    JVM_OPTS="$JVM_OPTS -XX:StartFlightRecording=duration=60s,filename=$LOG_DIR/flight-recorder.jfr"
fi

# ================================
# Spring Boot应用参数
# ================================

# Spring Boot参数
SPRING_OPTS="--spring.profiles.active=$PROFILE"
SPRING_OPTS="$SPRING_OPTS --server.port=8082"
SPRING_OPTS="$SPRING_OPTS --logging.file.name=$LOG_DIR/terminal-service-$PROFILE.log"

# ================================
# 启动应用
# ================================

JAVA_CMD="java $JVM_OPTS -jar $JAR_FILE $SPRING_OPTS"

echo "[INFO] JVM启动参数:"
echo "$JVM_OPTS" | tr ' ' '\n' | grep -E '^-' | sort

echo ""
echo "[INFO] 启动LED设备终端通信服务..."
echo "[INFO] 完整启动命令:"
echo "$JAVA_CMD"
echo ""

# 启动应用并获取PID
nohup $JAVA_CMD > "$LOG_DIR/startup-$PROFILE.log" 2>&1 &
APP_PID=$!

# 保存PID
echo $APP_PID > "$PID_FILE"

echo "[INFO] 服务启动中，PID: $APP_PID"
echo "[INFO] 启动日志: $LOG_DIR/startup-$PROFILE.log"
echo "[INFO] 应用日志: $LOG_DIR/terminal-service-$PROFILE.log"
echo "[INFO] GC日志: $GC_LOG_FILE"

# 等待服务启动
echo "[INFO] 等待服务启动..."
sleep 5

# 检查服务是否正常启动
if ps -p $APP_PID > /dev/null 2>&1; then
    echo "[SUCCESS] LED设备终端通信服务启动成功！"
    echo ""
    echo "服务信息："
    echo "  - PID: $APP_PID"
    echo "  - 环境: $PROFILE"
    echo "  - 端口: 8082"
    echo "  - 健康检查: curl http://localhost:8082/actuator/health"
    echo "  - WebSocket端点: ws://localhost:8082/terminal/ws"
    echo ""
    echo "停止服务: kill $APP_PID 或删除 $PID_FILE 后 kill $APP_PID"
else
    echo "[ERROR] 服务启动失败！"
    echo "[INFO] 请检查启动日志: $LOG_DIR/startup-$PROFILE.log"
    rm -f "$PID_FILE"
    exit 1
fi