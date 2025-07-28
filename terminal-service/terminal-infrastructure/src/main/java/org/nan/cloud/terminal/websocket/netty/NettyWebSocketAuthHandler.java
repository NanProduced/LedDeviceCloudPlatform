package org.nan.cloud.terminal.websocket.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.application.domain.TerminalAccount;
import org.nan.cloud.terminal.application.domain.TerminalInfo;
import org.nan.cloud.terminal.application.repository.TerminalRepository;
import org.nan.cloud.terminal.config.security.auth.TerminalPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
public class NettyWebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    private final TerminalRepository  terminalRepository;

    private final PasswordEncoder passwordEncoder;

    public NettyWebSocketAuthHandler(TerminalRepository terminalRepository, PasswordEncoder passwordEncoder) {
        this.terminalRepository = terminalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static final AttributeKey<TerminalPrincipal> TERMINAL_PRINCIPAL =
            AttributeKey.valueOf("terminalPrincipal");
    public static final AttributeKey<String> TERMINAL_NAME =
            AttributeKey.valueOf("terminalName");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof FullHttpRequest req&& req.uri().startsWith("/ColorWebSocket/websocket/chat")
                && req.method().equals(HttpMethod.GET)
                && HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(
                req.headers().get(HttpHeaderNames.UPGRADE))) {

            if (!authenticateRequest(ctx, req)) {
                // 认证失败，发送HTTP 401响应并关闭连接
                log.warn("WebSocket认证失败，发送401响应并关闭连接");
                sendErrorResponse(ctx);
                return;
            }

            // 移除查询参数，创建新的请求URI
            String uriWithoutParams = "/ColorWebSocket/websocket/chat";
            
            // 创建一个新的FullHttpRequest，URI中不包含查询参数
            DefaultFullHttpRequest newReq = new DefaultFullHttpRequest(
                req.protocolVersion(),
                req.method(),
                uriWithoutParams,
                req.content().retain(),
                req.headers(),
                req.trailingHeaders()
            );
            
            log.info("WebSocket认证成功后，修改请求URI: {} -> {}", req.uri(), uriWithoutParams);
            
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(newReq);
        }
        else ctx.fireChannelRead(msg);

    }

    private boolean authenticateRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        try {
            QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
            Map<String, List<String>> params = decoder.parameters();
            String username = params.getOrDefault("username", List.of("")).get(0);
            String password = params.getOrDefault("password", List.of("")).get(0);

            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                log.warn("WebSocket认证失败: 缺少用户名或密码, URI: {}, 解析到的参数: {}", req.uri(), params.keySet());
                return false;
            }

            TerminalAccount account = terminalRepository.getAccountByName(username);
            if (account == null) {
                log.warn("WebSocket认证失败: 账号用户名不存在: {}", username);
                return false;
            }

            if (!passwordEncoder.matches(password, account.getPassword())) {
                log.warn("WebSocket认证失败: 账号密码错误: {}", username);
                return false;
            }

            if (account.getStatus() != 0) {
                log.warn("WebSocket认证失败: 账号封禁: {}", username);
                return false;
            }

            TerminalInfo terminalInfo = terminalRepository.getInfoByTid(account.getTid());
            TerminalPrincipal principal = new TerminalPrincipal();
            principal.setTid(account.getTid());
            principal.setTerminalName(terminalInfo.getTerminalName());
            principal.setOid(terminalInfo.getOid());
            principal.setStatus(account.getStatus());

            // 将认证信息存储到Channel属性中
            ctx.channel().attr(TERMINAL_NAME).set(principal.getTerminalName());
            ctx.channel().attr(TERMINAL_PRINCIPAL).set(principal);

            log.info("WebSocket认证成功: username={}, tid={}, terminalName={}",
                    username, principal.getTid(), principal.getTerminalName());

            return true;
        } catch (Exception e) {
            log.error("WebSocket认证异常", e);
            return false;
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED,
                io.netty.buffer.Unpooled.copiedBuffer("认证失败", StandardCharsets.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
    }
}
