package org.nan.cloud.auth.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface AuthApi {

    String prefix = "/internal/auth";

    @PostMapping(prefix + "/encode")
    String encodePsw(@RequestParam("psw") String password);
}
