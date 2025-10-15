package org.nan.cloud.core.event;

import org.apache.commons.lang3.tuple.Pair;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.event.rbac.RemoveUserAndRoleRel;
import org.springframework.context.ApplicationEvent;

public class UserDeleteEvent extends ApplicationEvent implements RemoveUserAndRoleRel {

    private User user;

    private Long deletedByUser;

    public UserDeleteEvent(Object source, User user, Long deletedByUser) {
        super(source);
        this.user = user;
        this.deletedByUser = deletedByUser;
    }

    @Override
    public Pair<Long, Long> getOidAndUid() {
        return Pair.of(user.getOid(), user.getUid());
    }
}
