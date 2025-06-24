package org.nan.cloud.auth.api.client;

import org.nan.cloud.auth.api.AuthApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "auth-server")
public interface AuthClient extends AuthApi {


}
