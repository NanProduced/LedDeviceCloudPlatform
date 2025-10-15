package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.res.TaskInitResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public interface MaterialTranscodeApi {

    String prefix = "/material";

    @PostMapping(prefix + "/{mid}/transcode")
    TaskInitResponse submitTranscode(@PathVariable("mid") Long materialId);
}
