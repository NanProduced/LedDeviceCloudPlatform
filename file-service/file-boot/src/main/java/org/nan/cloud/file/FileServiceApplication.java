package org.nan.cloud.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 文件服务启动类
 * 
 * 功能说明：
 * - 文件上传和下载管理
 * - 基于FFmpeg的视频转码服务
 * - 多存储策略支持（本地存储、阿里云OSS）
 * - 分片上传和断点续传
 * - 实时转码进度监控
 * - 文件缩略图生成
 * - 文件权限和安全控制
 * 
 * 服务端口：8085
 * 
 * 核心特性：
 * - 支持多种文件格式（视频、图片、音频、文档等）
 * - GPU加速转码支持
 * - 异步任务处理
 * - 文件去重和版本管理
 * - WebSocket实时进度推送
 * - 高并发和大文件处理优化
 * 
 * TODO: 创建REST控制器实现API接口
 * TODO: 完善数据库配置和连接池设置
 * TODO: 添加Redis缓存配置
 * TODO: 配置异步任务执行器
 * TODO: 添加文件存储路径配置
 * TODO: 完善监控和健康检查端点
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "org.nan.cloud")
@EnableDiscoveryClient
public class FileServiceApplication {

    public static void main(String[] args) {
        // 添加启动前的系统信息
        log.info("========== 文件服务启动开始 ==========");
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {}", System.getProperty("os.name"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        log.info("可用处理器核心数: {}", Runtime.getRuntime().availableProcessors());
        log.info("最大堆内存: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        
        try {
            SpringApplication.run(FileServiceApplication.class, args);
            
            log.info("🚀 文件服务 (File Service) 启动成功! 端口: 8086");
            log.info("📁 文件上传接口: http://localhost:8086/file/upload");
            log.info("🎬 视频转码接口: http://localhost:8086/file/transcoding");
            log.info("📂 文件管理接口: http://localhost:8086/file/management");
            log.info("📋 管理端点: http://localhost:8086/actuator");
            log.info("📊 健康检查: http://localhost:8086/actuator/health");
            log.info("📚 API文档: http://localhost:8086/swagger-ui.html");
            log.info("⚡ 转码监控: http://localhost:8086/transcoding/monitor");
            log.info("========== 文件服务启动完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 文件服务启动失败！", e);
            System.exit(1);
        }
    }
}