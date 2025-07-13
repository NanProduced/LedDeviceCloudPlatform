package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.res.UserGroupTreeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

public interface UserGroupApi {

    String prefix = "/user-group";

    @GetMapping(prefix + "/tree/init")
    UserGroupTreeResponse getUserGroupTree();


}
