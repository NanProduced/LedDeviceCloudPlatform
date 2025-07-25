package org.nan.cloud.terminal.controller;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.config.security.auth.TerminalPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 
 * 用于验证Basic Auth认证是否生效
 * 提供简单的API端点测试认证流程和获取当前认证用户信息
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 测试认证是否生效
     * 
     * 访问此端点需要提供Basic Auth认证
     * 成功认证后返回当前登录的终端设备信息
     * 
     * @return 认证用户信息和测试结果
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> testAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now());
        result.put("authenticated", authentication.isAuthenticated());
        result.put("authType", authentication.getClass().getSimpleName());
        
        if (authentication.getPrincipal() instanceof TerminalPrincipal principal) {

            Map<String, Object> terminalInfo = new HashMap<>();
            terminalInfo.put("tid", principal.getTid());
            terminalInfo.put("terminalName", principal.getTerminalName());
            terminalInfo.put("oid", principal.getOid());
            terminalInfo.put("status", principal.getStatus());
            terminalInfo.put("authorities", principal.getAuthorities());
            
            result.put("terminal", terminalInfo);
            result.put("message", "Basic Auth认证成功");
            
            log.info("测试认证成功: tid={}, terminalName={}", 
                principal.getTid(), principal.getTerminalName());
        } else {
            result.put("principal", authentication.getPrincipal());
            result.put("message", "认证成功，但Principal类型异常");
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 公开访问的端点（无需认证）
     * 
     * 用于测试系统是否正常运行
     * 
     * @return 系统状态信息
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> testPublic() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "公开端点访问成功");
        result.put("service", "terminal-service");
        result.put("status", "running");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前认证用户详细信息
     * 
     * @return 当前认证用户的完整信息
     */
    @GetMapping("/user-info") 
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now());
        
        if (authentication != null && authentication.isAuthenticated()) {
            result.put("name", authentication.getName());
            result.put("authorities", authentication.getAuthorities());
            result.put("details", authentication.getDetails());
            result.put("authenticated", true);
            
            if (authentication.getPrincipal() instanceof TerminalPrincipal principal) {
                result.put("principalType", "TerminalPrincipal");
                result.put("terminalId", principal.getTid());
                result.put("terminalName", principal.getTerminalName());
                result.put("organizationId", principal.getOid());
                result.put("terminalStatus", principal.getStatus());
            } else {
                result.put("principalType", authentication.getPrincipal().getClass().getSimpleName());
                result.put("principal", authentication.getPrincipal().toString());
            }
        } else {
            result.put("authenticated", false);
            result.put("message", "未认证或认证已过期");
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试端点安全性
     * 
     * 模拟敏感操作，需要认证才能访问
     * 
     * @return 操作结果
     */
    @GetMapping("/secure")
    public ResponseEntity<Map<String, Object>> testSecure() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof TerminalPrincipal principal) {

            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", LocalDateTime.now());
            result.put("message", "安全端点访问成功");
            result.put("terminalId", principal.getTid());
            result.put("operation", "secure_test_passed");
            result.put("status", "success");
            
            log.info("安全端点访问: tid={}, terminalName={}", 
                principal.getTid(), principal.getTerminalName());
            
            return ResponseEntity.ok(result);
        }
        
        return ResponseEntity.status(401).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "message", "访问被拒绝：需要有效的终端设备认证",
            "status", "unauthorized"
        ));
    }
}