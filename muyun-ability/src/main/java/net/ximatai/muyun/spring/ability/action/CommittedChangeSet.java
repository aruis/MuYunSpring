package net.ximatai.muyun.spring.ability.action;

import java.util.List;

public record CommittedChangeSet(String changeSetId, List<DataChange> changes) {
    public CommittedChangeSet {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    public static CommittedChangeSet empty(String changeSetId) {
        return new CommittedChangeSet(changeSetId, List.of());
    }
}
