package org.nan.cloud.terminal.websocket.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.infrastructure.connection.ConnectionManager;
import org.nan.cloud.terminal.config.security.auth.TerminalPrincipal;
import org.nan.cloud.terminal.websocket.session.TerminalWebSocketSession;
import org.nan.cloud.terminal.cache.TerminalOnlineStatusManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket帧处理器
 * 
 * 处理WebSocket连接和消息：
 * 1. 连接建立：创建TerminalWebSocketSession
 * 2. 消息处理：处理文本和二进制消息
 * 3. 心跳检测：PING/PONG消息处理
 * 4. 连接关闭：清理资源
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class NettyWebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ConnectionManager connectionManager;
    private final StringRedisTemplate redisTemplate;
    private final TerminalOnlineStatusManager onlineStatusManager;

    // 本地会话缓存
    private final ConcurrentHashMap<String, TerminalWebSocketSession> localSessions = 
        new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("NettyWebSocketFrameHandler.channelActive被调用 - channel: {}", ctx.channel().id());
        // 不在这里检查认证信息，等待WebSocket握手完成后再处理
        super.channelActive(ctx);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("NettyWebSocketFrameHandler.userEventTriggered被调用: evt={}", evt.getClass().getSimpleName());
        
        // 监听WebSocket握手完成事件（标准Netty机制）
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {
            log.info("WebSocket握手完成事件，开始初始化会话");
            log.info("握手详细信息: requestUri={}, requestHeaders={}, selectedSubprotocol={}",
                handshake.requestUri(), handshake.requestHeaders().names(), handshake.selectedSubprotocol());
            initializeWebSocketSession(ctx);
        }
        
        super.userEventTriggered(ctx, evt);
    }
    
    /**
     * WebSocket会话初始化方法
     * 在握手完成事件中被调用，从Channel属性获取认证信息并创建会话
     */
    public void initializeWebSocketSession(ChannelHandlerContext ctx) {
        // 从Channel属性中获取认证信息
        String terminalName = ctx.channel().attr(NettyWebSocketAuthHandler.TERMINAL_NAME).get();
        TerminalPrincipal principal = ctx.channel().attr(NettyWebSocketAuthHandler.TERMINAL_PRINCIPAL).get();
        
        // 调试日志：显示认证信息状态
        log.info("初始化WebSocket会话 - 认证信息检查: terminalName={}, principal={}", 
            terminalName, principal != null ? "存在" : "null");
        
        if (terminalName == null || principal == null) {
            log.error("WebSocket会话初始化失败: 缺少认证信息 - terminalName={}, principal={}", 
                terminalName, principal != null ? "存在" : "null");
            ctx.close();
            return;
        }

        Long tid = principal.getTid();
        Long oid = principal.getOid();
        String sessionId = ctx.channel().id().asShortText();
        String clientIp = getClientIp(ctx);

        // 创建终端会话
        TerminalWebSocketSession terminalSession = TerminalWebSocketSession.builder()
            .sessionId(sessionId)
            .tid(tid)
            .oid(oid)
            .connectTime(System.currentTimeMillis())
            .clientIp(clientIp)
            .lastHeartbeatTime(System.currentTimeMillis())
            .build();

        // 存储NettyChannel引用
        terminalSession.setNettyChannel(ctx.channel());

        // 添加到连接管理器
        connectionManager.addConnection(tid, terminalSession);
        localSessions.put(sessionId, terminalSession);

        log.info("Netty WebSocket连接建立成功: username={}, tid={}, terminalName={}, sessionId={}, clientIp={}",
            terminalName, principal.getTid(), principal.getTerminalName(), sessionId, clientIp);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        String sessionId = ctx.channel().id().asShortText();
        TerminalWebSocketSession terminalSession = localSessions.get(sessionId);
        
        if (terminalSession == null) {
            log.warn("收到消息但会话不存在: sessionId={}", sessionId);
            return;
        }

        Long tid = terminalSession.getTid();
        Long oid = terminalSession.getOid();
        
        // 更新心跳时间
        terminalSession.setLastHeartbeatTime(System.currentTimeMillis());
        
        // 更新终端活跃状态到Redis (用于离线检测)
        onlineStatusManager.updateTerminalActivity(oid, tid);

        // 处理不同类型的WebSocket帧
        if (frame instanceof TextWebSocketFrame) {
            String message = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, terminalSession, message);
            
        } else if (frame instanceof BinaryWebSocketFrame) {
            byte[] data = new byte[frame.content().readableBytes()];
            frame.content().readBytes(data);
            handleBinaryMessage(ctx, terminalSession, data);
            
        } else if (frame instanceof PingWebSocketFrame) {
            // 响应PING消息
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            
        } else if (frame instanceof PongWebSocketFrame) {
            // PONG消息，更新心跳时间即可
            log.debug("收到PONG消息: tid={}", tid);
            
        } else if (frame instanceof CloseWebSocketFrame) {
            // 关闭帧
            log.info("收到关闭帧: tid={}", tid);
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().id().asShortText();
        TerminalWebSocketSession terminalSession = localSessions.remove(sessionId);
        
        if (terminalSession != null) {
            Long tid = terminalSession.getTid();
            
            // 从连接管理器移除
            connectionManager.removeConnection(tid, sessionId);
            
            log.info("Netty WebSocket连接关闭: tid={}, sessionId={}", tid, sessionId);
        }
        
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String sessionId = ctx.channel().id().asShortText();
        log.error("WebSocket处理异常: sessionId={}", sessionId, cause);
        ctx.close();
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(ChannelHandlerContext ctx, TerminalWebSocketSession session, String message) {
        Long tid = session.getTid();
        
        try {
            // 处理心跳消息
            if ("PING".equals(message)) {
                ctx.writeAndFlush(new TextWebSocketFrame("PONG"));
                return;
            }
            
            // 处理业务消息
            handleBusinessMessage(session, message);
            
            log.debug("处理文本消息: tid={}, messageLength={}", tid, message.length());
            
        } catch (Exception e) {
            log.error("处理文本消息异常: tid={}", tid, e);
        }
    }

    /**
     * 处理二进制消息
     */
    private void handleBinaryMessage(ChannelHandlerContext ctx, TerminalWebSocketSession session, byte[] data) {
        Long tid = session.getTid();
        
        try {
            log.debug("处理二进制消息: tid={}, dataLength={}", tid, data.length);
            
            // TODO: 根据需要处理二进制数据
            
        } catch (Exception e) {
            log.error("处理二进制消息异常: tid={}", tid, e);
        }
    }

    /**
     * 处理业务消息
     */
    private void handleBusinessMessage(TerminalWebSocketSession session, String message) {
        try {
            Long tid = session.getTid();
            
            // 记录消息到Redis
            String messageKey = "terminal:message:" + tid + ":" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(messageKey, message, 1, TimeUnit.HOURS);
            
            // TODO: 实现具体的业务逻辑
            // 1. 设备指令处理
            // 2. 状态上报处理
            // 3. 数据同步处理
            
        } catch (Exception e) {
            log.error("处理业务消息异常: tid={}", session.getTid(), e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ChannelHandlerContext ctx) {
        try {
            return ctx.channel().remoteAddress().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}