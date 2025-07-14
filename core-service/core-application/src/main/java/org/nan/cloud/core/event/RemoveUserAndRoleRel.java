package org.nan.cloud.core.event;

import org.apache.commons.lang3.tuple.Pair;

public interface RemoveUserAndRoleRel {

    Pair<Long, Long> getOidAndUid();
}
