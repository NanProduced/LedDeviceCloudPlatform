package org.nan.cloud.terminal.config.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.infrastructure.mapper.auth.TerminalAccountMapper;
import org.nan.cloud.terminal.application.repository.TerminalRepository;
import org.nan.cloud.terminal.websocket.netty.NettyWebSocketAuthHandler;
import org.nan.cloud.terminal.websocket.netty.NettyWebSocketFrameHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * 独立的Netty WebSocket服务器
 * 
 * 在8843端口启动独立的WebSocket服务器：
 * 1. 端口隔离：8843专用于WebSocket，8085用于HTTP
 * 2. 高性能：基于Netty NIO事件循环
 * 3. 路径：/ColorWebSocket/websocket/chat
 * 4. 认证：URL参数username/password验证
 * 5. 生命周期：Spring容器自动管理启动和关闭
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyWebSocketServer implements ApplicationRunner {

    @Value("${terminal.websocket.port:8843}")
    private int websocketPort;

    private final NettyWebSocketFrameHandler frameHandler;
    private final TerminalRepository terminalRepository;
    private final PasswordEncoder passwordEncoder;


    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        startWebSocketServer();
    }

    /**
     * 启动WebSocket服务器
     */
    private void startWebSocketServer() {
        // 创建事件循环组
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        log.info("initChannel被调用 - 开始配置pipeline for channel: {}", ch.id());
                        ChannelPipeline pipeline = ch.pipeline();

                        // 1) HTTP 解码/编码
                        pipeline.addLast(new HttpServerCodec());
                        // 2) 聚合成 FullHttpRequest
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // 3) 握手前的认证拦截
                        pipeline.addLast(new NettyWebSocketAuthHandler(terminalRepository, passwordEncoder));
                        // 4) WebSocket 协议处理：path 必须和客户端 URL path 对应
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ColorWebSocket/websocket/chat", null, true));
                        // 5) WebSocket帧处理器
                        pipeline.addLast(frameHandler);
                        
                        log.info("pipeline配置完成，当前handlers: {}", pipeline.names());
                    }
                });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(websocketPort).sync();
            serverChannel = future.channel();
            
            log.info("Netty WebSocket服务器启动成功: 端口={}, 路径=/ColorWebSocket/websocket/chat", websocketPort);
            log.info("客户端连接URL: ws://host:{}/ColorWebSocket/websocket/chat?username=xxx&password=xxx", websocketPort);
            
            // 异步等待服务器关闭
            new Thread(() -> {
                try {
                    serverChannel.closeFuture().sync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("WebSocket服务器关闭等待被中断");
                }
            }, "websocket-server-shutdown").start();
            
        } catch (Exception e) {
            log.error("WebSocket服务器启动失败", e);
            shutdown();
        }
    }

    /**
     * 关闭WebSocket服务器
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭Netty WebSocket服务器...");
        
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        log.info("Netty WebSocket服务器已关闭");
    }
}