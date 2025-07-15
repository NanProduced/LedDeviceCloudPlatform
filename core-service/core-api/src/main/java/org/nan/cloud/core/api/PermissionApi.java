package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.res.PermissionResponse;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

public interface PermissionApi {

    String prefix = "/permission";

    @GetMapping(prefix + "/get")
    Map<String, List<PermissionResponse>> getCurUserPermissions();
}
