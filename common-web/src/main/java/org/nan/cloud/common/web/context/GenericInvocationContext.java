package org.nan.cloud.common.web.context;

import lombok.Data;
import org.nan.cloud.common.basic.utils.JsonUtils;

import java.io.Serializable;
import java.util.Objects;

@Data
public class GenericInvocationContext implements Serializable {

    private static final long serialVersionUID = -1038504233980432399L;

    private RequestUserInfo requestUser;

    private String traceId;

    public void setRequestUser(String jsonNode) {
        RequestUserInfo requestUserInfo = JsonUtils.fromJson(jsonNode, RequestUserInfo.class);
        setRequestUser(requestUserInfo);
    }

    public String getRequestUserAsJson() {
        if (Objects.isNull(requestUser)) {
            return null;
        }
        return JsonUtils.toJson(requestUser);
    }

    public void setRequestUser(RequestUserInfo requestUser) {
        this.requestUser = requestUser;
    }

    public boolean isUserRequest() {
        return Objects.nonNull(requestUser);
    }
}