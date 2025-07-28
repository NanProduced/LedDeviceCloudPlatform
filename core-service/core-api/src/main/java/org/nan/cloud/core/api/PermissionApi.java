package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.res.OperationPermissionResponse;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

public interface PermissionApi {

    String prefix = "/operation_permission";

    @GetMapping(prefix + "/get")
    Map<String, List<OperationPermissionResponse>> getCurUserPermissions();
}
