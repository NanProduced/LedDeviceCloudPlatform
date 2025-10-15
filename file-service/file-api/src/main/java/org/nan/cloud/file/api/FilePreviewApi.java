package org.nan.cloud.file.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 文件预览API接口
 */
public interface FilePreviewApi {

    String prefix = "/file/preview";

    @GetMapping(prefix + "/{fileId}")
    void previewFile(
            @PathVariable String fileId,

            @RequestParam(required = false) Integer w,

            @RequestParam(required = false) Integer h,

            @RequestParam(required = false, defaultValue = "cover") String fit,

            @RequestParam(required = false, defaultValue = "jpg") String format,

            @RequestParam(required = false, defaultValue = "85") Integer q,

            @RequestParam(required = false, defaultValue = "1.0") Double t,

            @RequestParam(required = false) Integer frame,

            HttpServletRequest request,
            HttpServletResponse response);

    @GetMapping(prefix + "/stream/{fileId}")
    ResponseEntity<?> streamFile(
            @PathVariable String fileId,
            HttpServletRequest request,
            HttpServletResponse response);

    @GetMapping(prefix + "/download/{fileId}")
    void downloadFile(
            @PathVariable String fileId,
            @RequestParam(required = false, defaultValue = "true") Boolean attachment,
            HttpServletRequest request,
            HttpServletResponse response);

}
