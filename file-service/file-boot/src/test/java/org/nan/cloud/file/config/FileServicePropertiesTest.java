package org.nan.cloud.file.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件服务配置属性测试
 */
@SpringBootTest
@TestPropertySource(properties = {
        "file.storage.default-strategy=LOCAL",
        "file.transcoding.ffmpeg.path=/usr/bin/ffmpeg",
        "file.transcoding.ffmpeg.threads=4"
})
class FileServicePropertiesTest {

    @Autowired
    private FileServiceProperties fileServiceProperties;

    @Test
    void testStorageProperties() {
        assertEquals("LOCAL", fileServiceProperties.getStorage().getDefaultStrategy());
        assertEquals("/data/files", fileServiceProperties.getStorage().getLocal().getBasePath());
    }

    @Test
    void testTranscodingProperties() {
        assertEquals("/usr/bin/ffmpeg", fileServiceProperties.getTranscoding().getFfmpeg().getPath());
        assertEquals(4, fileServiceProperties.getTranscoding().getFfmpeg().getThreads());
        assertTrue(fileServiceProperties.getTranscoding().getFfmpeg().isEnableGpu());
    }
}