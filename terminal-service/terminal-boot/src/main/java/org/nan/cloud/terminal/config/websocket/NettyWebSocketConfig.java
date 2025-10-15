package org.nan.cloud.terminal.config.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Netty WebSocket性能优化配置
 * 
 * 针对高并发WebSocket连接场景的Netty参数调优：
 * 1. 连接队列优化：SO_BACKLOG=32768，支持大量并发连接请求
 * 2. 内存池优化：PooledByteBufAllocator，减少GC压力和内存分配开销
 * 3. 事件循环优化：根据CPU核数配置线程池，提升并发处理能力
 * 4. TCP参数调优：启用TCP_NODELAY，关闭Nagle算法，降低延迟
 * 5. 系统适配：自动检测Linux epoll，提升I/O性能
 * 
 * 性能目标：
 * - 支持10,000并发WebSocket连接
 * - 单连接延迟 < 10ms，批量处理 < 50ms
 * - 内存使用：768KB per 10k connections
 * - CPU效率：多核负载均衡，单核利用率 < 80%
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class NettyWebSocketConfig {

    private final Environment environment;

    public NettyWebSocketConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Netty服务器定制器 - 高性能参数配置
     */
    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        
        // 添加自定义的Netty服务器配置
        factory.addServerCustomizers(nettyServerCustomizer());
        
        return factory;
    }

    /**
     * Netty服务器自定义配置
     */
    @Bean
    public NettyServerCustomizer nettyServerCustomizer() {
        return httpServer -> {
            // 获取系统配置
            boolean useEpoll = isLinuxEpollAvailable();
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            
            log.info("Netty配置初始化: 系统={}, CPU核数={}, 使用Epoll={}", 
                System.getProperty("os.name"), availableProcessors, useEpoll);

            return httpServer
                // 1. 连接队列优化 - 支持大量并发连接
                .option(ChannelOption.SO_BACKLOG, 32768)
                
                // 2. TCP参数优化 - 降低网络延迟
                .option(ChannelOption.TCP_NODELAY, true)      // 禁用Nagle算法
                .option(ChannelOption.SO_REUSEADDR, true)     // 地址复用
                .option(ChannelOption.SO_KEEPALIVE, true)     // TCP Keep-Alive
                
                // 3. 内存优化 - 使用池化内存分配器
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                
                // 4. 接收缓冲区优化 - 适配高吞吐场景
                .option(ChannelOption.SO_RCVBUF, 65536)       // 64KB接收缓冲区
                .option(ChannelOption.SO_SNDBUF, 65536)       // 64KB发送缓冲区  
                .childOption(ChannelOption.SO_RCVBUF, 32768)  // 子连接32KB接收缓冲区
                .childOption(ChannelOption.SO_SNDBUF, 32768)  // 子连接32KB发送缓冲区
                
                // 5. 连接超时配置
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)  // 30秒连接超时
                
                // 6. 自动读取配置 - 支持背压控制
                .childOption(ChannelOption.AUTO_READ, true)
                
                // 7. 写缓冲水位线 - 防止内存溢出
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, 
                    new io.netty.channel.WriteBufferWaterMark(32 * 1024, 64 * 1024));
        };
    }

    /**
     * 自定义ServerBootstrap配置（如果需要更精细的控制）
     */
    @Bean
    public ServerBootstrap customServerBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        // 根据系统选择最优的EventLoopGroup
        EventLoopGroup bossGroup = createEventLoopGroup(1, "netty-boss");
        EventLoopGroup workerGroup = createEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2, "netty-worker");
        
        // 选择最优的Channel类型
        Class<?> channelClass = isLinuxEpollAvailable() ? 
            EpollServerSocketChannel.class : NioServerSocketChannel.class;
        
        bootstrap.group(bossGroup, workerGroup)
            .channel((Class) channelClass)
            
            // Boss线程配置 - 处理新连接
            .option(ChannelOption.SO_BACKLOG, 32768)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            
            // Worker线程配置 - 处理I/O事件
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.SO_RCVBUF, 32768)
            .childOption(ChannelOption.SO_SNDBUF, 32768);
        
        log.info("自定义ServerBootstrap配置完成: Boss线程=1, Worker线程={}, Channel={}",
            Runtime.getRuntime().availableProcessors() * 2, channelClass.getSimpleName());
        
        return bootstrap;
    }

    /**
     * 创建EventLoopGroup - 根据系统选择最优实现
     */
    private EventLoopGroup createEventLoopGroup(int threadCount, String threadNamePrefix) {
        if (isLinuxEpollAvailable()) {
            log.info("使用EpollEventLoopGroup: 线程数={}, 前缀={}", threadCount, threadNamePrefix);
            return new EpollEventLoopGroup(threadCount, r -> {
                Thread t = new Thread(r, threadNamePrefix + "-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
        } else {
            log.info("使用NioEventLoopGroup: 线程数={}, 前缀={}", threadCount, threadNamePrefix);
            return new NioEventLoopGroup(threadCount, r -> {
                Thread t = new Thread(r, threadNamePrefix + "-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
        }
    }

    /**
     * 检查是否支持Linux Epoll
     * Epoll在Linux系统上提供更好的I/O性能
     */
    private boolean isLinuxEpollAvailable() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isLinux = osName.contains("linux");
            
            if (isLinux) {
                // 尝试加载Epoll类
                Class.forName("io.netty.channel.epoll.EpollEventLoopGroup");
                log.info("检测到Linux系统且Epoll可用，将使用高性能Epoll模式");
                return true;
            }
        } catch (ClassNotFoundException e) {
            log.warn("Epoll类不可用，回退到NIO模式: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Epoll可用性检查异常，回退到NIO模式", e);
        }
        
        return false;
    }

    /**
     * JVM参数建议配置（通过日志输出，需要在启动参数中配置）
     */
    public void logRecommendedJvmParams() {
        log.info("=== Netty高性能WebSocket推荐JVM参数 ===");
        log.info("-server");
        log.info("-Xms8g -Xmx16g");
        log.info("-XX:+UseG1GC");
        log.info("-XX:MaxGCPauseMillis=200");
        log.info("-XX:G1HeapRegionSize=16m");
        log.info("-XX:+G1UseAdaptiveIHOP");
        log.info("-XX:G1MixedGCCountTarget=8");
        log.info("-XX:G1OldCSetRegionThreshold=10");
        log.info("-Dio.netty.allocator.type=pooled");
        log.info("-Dio.netty.allocator.numDirectArenas=16");
        log.info("-Dio.netty.allocator.numHeapArenas=16");
        log.info("-Dio.netty.allocator.pageSizes=8192,16384,32768");
        log.info("-Dio.netty.allocator.maxCachedBufferCapacity=32768");
        log.info("-Dio.netty.allocator.cacheTrimInterval=600");
        log.info("==========================================");
    }
}