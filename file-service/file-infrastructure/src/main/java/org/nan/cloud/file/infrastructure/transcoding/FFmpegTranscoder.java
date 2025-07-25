package org.nan.cloud.file.infrastructure.transcoding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FFmpeg转码引擎实现
 * 
 * 基于FFmpeg命令行工具的视频转码实现：
 * - 支持多种视频格式转换
 * - GPU加速支持
 * - 实时进度监控
 * - 自动参数优化
 * - 异常处理和重试机制
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FFmpegTranscoder {

    @Value("${file.transcoding.ffmpeg.path:/usr/local/bin/ffmpeg}")
    private String ffmpegPath;

    @Value("${file.transcoding.ffmpeg.threads:8}")
    private int defaultThreads;

    @Value("${file.transcoding.ffmpeg.timeout:3600}")
    private int timeoutSeconds;

    @Value("${file.transcoding.ffmpeg.enable-gpu:true}")
    private boolean enableGpu;

    @Value("${file.transcoding.temp.dir:/tmp/transcoding}")
    private String tempDir;

    // 进度解析正则表达式
    private static final Pattern PROGRESS_PATTERN = Pattern.compile(
            "frame=\\s*(\\d+)\\s+fps=\\s*([\\d.]+)\\s+.*time=([\\d:.]+)"
    );

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "Duration: ([\\d:.]+)"
    );

    /**
     * 转码视频文件
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param config 转码配置
     * @param progressCallback 进度回调
     * @return 转码结果
     */
    public TranscodingResult transcode(String inputPath, String outputPath, 
                                     TranscodingConfig config, 
                                     ProgressCallback progressCallback) {
        log.info("开始转码 - 输入: {}, 输出: {}", inputPath, outputPath);

        try {
            // 1. 构建FFmpeg命令
            List<String> command = buildFFmpegCommand(inputPath, outputPath, config);
            
            // 2. 创建进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            // 3. 设置工作目录
            processBuilder.directory(new File(tempDir));

            // 4. 启动进程
            Process process = processBuilder.start();

            // 5. 异步监控进度
            CompletableFuture<Void> progressMonitor = monitorProgress(
                    process.getInputStream(), progressCallback);

            // 6. 等待转码完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("转码超时，已终止进程");
            }

            int exitCode = process.exitValue();
            progressMonitor.join();

            // 7. 检查转码结果
            if (exitCode == 0) {
                log.info("转码成功完成 - 输出: {}", outputPath);
                return TranscodingResult.success(outputPath, getOutputFileSize(outputPath));
            } else {
                String errorMessage = "FFmpeg进程异常退出，退出码: " + exitCode;
                log.error("转码失败 - {}", errorMessage);
                return TranscodingResult.failure(errorMessage);
            }

        } catch (Exception e) {
            log.error("转码过程中发生异常", e);
            return TranscodingResult.failure("转码异常: " + e.getMessage());
        }
    }

    /**
     * 获取视频信息
     * 
     * @param filePath 视频文件路径
     * @return 视频信息
     */
    public VideoInfo getVideoInfo(String filePath) {
        log.debug("获取视频信息 - 文件: {}", filePath);

        try {
            List<String> command = List.of(
                    ffmpegPath,
                    "-i", filePath,
                    "-hide_banner"
            );

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor(30, TimeUnit.SECONDS);

            return parseVideoInfo(output.toString());

        } catch (Exception e) {
            log.error("获取视频信息失败 - 文件: {}", filePath, e);
            throw new RuntimeException("获取视频信息失败", e);
        }
    }

    /**
     * 检查FFmpeg是否可用
     * 
     * @return 是否可用
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath, "-version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取支持的编码器列表
     * 
     * @return 编码器列表
     */
    public List<String> getSupportedEncoders() {
        try {
            List<String> command = List.of(ffmpegPath, "-encoders", "-hide_banner");
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            List<String> encoders = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("V") && line.contains("=")) {
                        String[] parts = line.split("\\s+", 3);
                        if (parts.length >= 2) {
                            encoders.add(parts[1]);
                        }
                    }
                }
            }

            process.waitFor(30, TimeUnit.SECONDS);
            return encoders;

        } catch (Exception e) {
            log.error("获取支持的编码器列表失败", e);
            return List.of();
        }
    }

    // 私有方法

    /**
     * 构建FFmpeg命令
     */
    private List<String> buildFFmpegCommand(String inputPath, String outputPath, TranscodingConfig config) {
        List<String> command = new ArrayList<>();

        // 基础命令
        command.add(ffmpegPath);
        command.add("-y"); // 覆盖输出文件
        command.add("-hide_banner");

        // GPU加速设置
        if (config.isEnableGpuAcceleration() && enableGpu) {
            command.add("-hwaccel");
            command.add("cuda");
        }

        // 内存优化参数
        command.add("-probesize");
        command.add("50M");
        command.add("-analyzeduration");
        command.add("100M");

        // 输入文件
        command.add("-i");
        command.add(inputPath);

        // 视频编码参数
        if (config.getVideoCodec() != null) {
            command.add("-c:v");
            if (config.isEnableGpuAcceleration() && enableGpu && "libx264".equals(config.getVideoCodec())) {
                command.add("h264_nvenc"); // 使用NVIDIA GPU编码器
            } else {
                command.add(config.getVideoCodec());
            }
        }

        // 音频编码参数
        if (config.getAudioCodec() != null) {
            command.add("-c:a");
            command.add(config.getAudioCodec());
        }

        // 分辨率设置
        if (config.getWidth() != null && config.getHeight() != null) {
            command.add("-s");
            command.add(config.getWidth() + "x" + config.getHeight());
        }

        // 帧率设置
        if (config.getFrameRate() != null) {
            command.add("-r");
            command.add(String.valueOf(config.getFrameRate()));
        }

        // 比特率设置
        if (config.getVideoBitrate() != null) {
            command.add("-b:v");
            command.add(config.getVideoBitrate() + "k");
        }

        if (config.getAudioBitrate() != null) {
            command.add("-b:a");
            command.add(config.getAudioBitrate() + "k");
        }

        // CRF质量设置
        if (config.getCrf() != null) {
            command.add("-crf");
            command.add(String.valueOf(config.getCrf()));
        }

        // 预设设置
        if (config.getPreset() != null) {
            command.add("-preset");
            command.add(config.getPreset());
        }

        // GOP设置
        if (config.getGopSize() != null) {
            command.add("-g");
            command.add(String.valueOf(config.getGopSize()));
        }

        // 线程数设置
        command.add("-threads");
        command.add(String.valueOf(config.getThreads() != null ? config.getThreads() : defaultThreads));

        // 音频移除
        if (config.isRemoveAudio()) {
            command.add("-an");
        }

        // 视频移除
        if (config.isRemoveVideo()) {
            command.add("-vn");
        }

        // 时间截取
        if (config.getStartTime() != null) {
            command.add("-ss");
            command.add(formatTime(config.getStartTime()));
        }

        if (config.getDuration() != null) {
            command.add("-t");
            command.add(formatTime(config.getDuration()));
        }

        // 优化参数
        command.add("-movflags");
        command.add("+faststart"); // 优化网络播放

        // 进度输出
        command.add("-progress");
        command.add("pipe:1");

        // 输出文件
        command.add(outputPath);

        log.debug("FFmpeg命令: {}", String.join(" ", command));
        return command;
    }

    /**
     * 监控转码进度
     */
    private CompletableFuture<Void> monitorProgress(InputStream inputStream, ProgressCallback callback) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                Long totalDuration = null;
                
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg输出: {}", line);

                    // 解析总时长
                    if (totalDuration == null) {
                        Matcher durationMatcher = DURATION_PATTERN.matcher(line);
                        if (durationMatcher.find()) {
                            totalDuration = parseTimeToSeconds(durationMatcher.group(1));
                        }
                    }

                    // 解析当前进度
                    Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
                    if (progressMatcher.find() && totalDuration != null) {
                        String currentTimeStr = progressMatcher.group(3);
                        long currentTime = parseTimeToSeconds(currentTimeStr);
                        
                        int progress = (int) ((currentTime * 100) / totalDuration);
                        progress = Math.min(100, Math.max(0, progress));

                        String fps = progressMatcher.group(2);
                        callback.onProgress(progress, fps, currentTimeStr);
                    }
                }
            } catch (IOException e) {
                log.error("进度监控异常", e);
            }
        });
    }

    /**
     * 解析视频信息
     */
    private VideoInfo parseVideoInfo(String ffmpegOutput) {
        VideoInfo.VideoInfoBuilder builder = VideoInfo.builder();

        // 解析时长
        Matcher durationMatcher = DURATION_PATTERN.matcher(ffmpegOutput);
        if (durationMatcher.find()) {
            builder.duration(parseTimeToSeconds(durationMatcher.group(1)));
        }

        // 解析分辨率和其他信息
        Pattern videoPattern = Pattern.compile("Stream #\\d+:\\d+.*Video: (\\w+).*?(\\d+)x(\\d+).*?(\\d+(?:\\.\\d+)?) fps");
        Matcher videoMatcher = videoPattern.matcher(ffmpegOutput);
        if (videoMatcher.find()) {
            builder.codec(videoMatcher.group(1))
                   .width(Integer.parseInt(videoMatcher.group(2)))
                   .height(Integer.parseInt(videoMatcher.group(3)))
                   .frameRate(Double.parseDouble(videoMatcher.group(4)));
        }

        // 解析比特率
        Pattern bitratePattern = Pattern.compile("bitrate: (\\d+) kb/s");
        Matcher bitrateMatcher = bitratePattern.matcher(ffmpegOutput);
        if (bitrateMatcher.find()) {
            builder.bitrate(Long.parseLong(bitrateMatcher.group(1)));
        }

        return builder.build();
    }

    /**
     * 时间格式转换为秒
     */
    private long parseTimeToSeconds(String timeStr) {
        String[] parts = timeStr.split(":");
        if (parts.length == 3) {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            return (long) (hours * 3600 + minutes * 60 + seconds);
        }
        return 0;
    }

    /**
     * 秒格式化为时间字符串
     */
    private String formatTime(Double seconds) {
        if (seconds == null) return "00:00:00";
        
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * 获取输出文件大小
     */
    private long getOutputFileSize(String outputPath) {
        try {
            Path path = Paths.get(outputPath);
            return java.nio.file.Files.size(path);
        } catch (Exception e) {
            log.warn("获取输出文件大小失败: {}", outputPath, e);
            return 0L;
        }
    }

    /**
     * 进度回调接口
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int progress, String fps, String currentTime);
    }

    /**
     * 转码配置
     */
    public static class TranscodingConfig {
        private String videoCodec;
        private String audioCodec;
        private Integer width;
        private Integer height;
        private Double frameRate;
        private Integer videoBitrate;
        private Integer audioBitrate;
        private Integer crf;
        private String preset;
        private Integer gopSize;
        private Integer threads;
        private boolean enableGpuAcceleration;
        private boolean removeAudio;
        private boolean removeVideo;
        private Double startTime;
        private Double duration;

        // 建造者模式构造器和getter/setter方法
        public static TranscodingConfig.Builder builder() {
            return new TranscodingConfig.Builder();
        }

        public static class Builder {
            private final TranscodingConfig config = new TranscodingConfig();

            public Builder videoCodec(String videoCodec) {
                config.videoCodec = videoCodec;
                return this;
            }

            public Builder audioCodec(String audioCodec) {
                config.audioCodec = audioCodec;
                return this;
            }

            public Builder resolution(Integer width, Integer height) {
                config.width = width;
                config.height = height;
                return this;
            }

            public Builder frameRate(Double frameRate) {
                config.frameRate = frameRate;
                return this;
            }

            public Builder videoBitrate(Integer videoBitrate) {
                config.videoBitrate = videoBitrate;
                return this;
            }

            public Builder audioBitrate(Integer audioBitrate) {
                config.audioBitrate = audioBitrate;
                return this;
            }

            public Builder crf(Integer crf) {
                config.crf = crf;
                return this;
            }

            public Builder preset(String preset) {
                config.preset = preset;
                return this;
            }

            public Builder gopSize(Integer gopSize) {
                config.gopSize = gopSize;
                return this;
            }

            public Builder threads(Integer threads) {
                config.threads = threads;
                return this;
            }

            public Builder enableGpuAcceleration(boolean enable) {
                config.enableGpuAcceleration = enable;
                return this;
            }

            public Builder removeAudio(boolean remove) {
                config.removeAudio = remove;
                return this;
            }

            public Builder removeVideo(boolean remove) {
                config.removeVideo = remove;
                return this;
            }

            public Builder timeRange(Double startTime, Double duration) {
                config.startTime = startTime;
                config.duration = duration;
                return this;
            }

            public TranscodingConfig build() {
                return config;
            }
        }

        // Getter方法
        public String getVideoCodec() { return videoCodec; }
        public String getAudioCodec() { return audioCodec; }
        public Integer getWidth() { return width; }
        public Integer getHeight() { return height; }
        public Double getFrameRate() { return frameRate; }
        public Integer getVideoBitrate() { return videoBitrate; }
        public Integer getAudioBitrate() { return audioBitrate; }
        public Integer getCrf() { return crf; }
        public String getPreset() { return preset; }
        public Integer getGopSize() { return gopSize; }
        public Integer getThreads() { return threads; }
        public boolean isEnableGpuAcceleration() { return enableGpuAcceleration; }
        public boolean isRemoveAudio() { return removeAudio; }
        public boolean isRemoveVideo() { return removeVideo; }
        public Double getStartTime() { return startTime; }
        public Double getDuration() { return duration; }
    }

    /**
     * 转码结果
     */
    public static class TranscodingResult {
        private final boolean success;
        private final String outputPath;
        private final String errorMessage;
        private final long outputFileSize;

        private TranscodingResult(boolean success, String outputPath, String errorMessage, long outputFileSize) {
            this.success = success;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
            this.outputFileSize = outputFileSize;
        }

        public static TranscodingResult success(String outputPath, long outputFileSize) {
            return new TranscodingResult(true, outputPath, null, outputFileSize);
        }

        public static TranscodingResult failure(String errorMessage) {
            return new TranscodingResult(false, null, errorMessage, 0);
        }

        public boolean isSuccess() { return success; }
        public String getOutputPath() { return outputPath; }
        public String getErrorMessage() { return errorMessage; }
        public long getOutputFileSize() { return outputFileSize; }
    }

    /**
     * 视频信息
     */
    public static class VideoInfo {
        private String codec;
        private Integer width;
        private Integer height;
        private Double frameRate;
        private Long bitrate;
        private Long duration;

        private VideoInfo(Builder builder) {
            this.codec = builder.codec;
            this.width = builder.width;
            this.height = builder.height;
            this.frameRate = builder.frameRate;
            this.bitrate = builder.bitrate;
            this.duration = builder.duration;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String codec;
            private Integer width;
            private Integer height;
            private Double frameRate;
            private Long bitrate;
            private Long duration;

            public Builder codec(String codec) { this.codec = codec; return this; }
            public Builder width(Integer width) { this.width = width; return this; }
            public Builder height(Integer height) { this.height = height; return this; }
            public Builder frameRate(Double frameRate) { this.frameRate = frameRate; return this; }
            public Builder bitrate(Long bitrate) { this.bitrate = bitrate; return this; }
            public Builder duration(Long duration) { this.duration = duration; return this; }

            public VideoInfo build() {
                return new VideoInfo(this);
            }
        }

        // Getter方法
        public String getCodec() { return codec; }
        public Integer getWidth() { return width; }
        public Integer getHeight() { return height; }
        public Double getFrameRate() { return frameRate; }
        public Long getBitrate() { return bitrate; }
        public Long getDuration() { return duration; }
    }
}