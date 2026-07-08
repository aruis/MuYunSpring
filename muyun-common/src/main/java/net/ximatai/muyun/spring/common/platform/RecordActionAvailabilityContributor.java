package net.ximatai.muyun.spring.common.platform;

import java.util.Optional;

public interface RecordActionAvailabilityContributor {
    Optional<RecordActionAvailabilityDecision> availability(String moduleAlias,
                                                            String actionCode,
                                                            String recordId);
}
