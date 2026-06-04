package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public interface SortWeb<T extends SortCapable, S extends SortAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    default WebCountResponse sort(@PathVariable String id,
                                  @RequestBody(required = false) SortWebRequest request) {
        return webScope(() -> {
            SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
            if (normalized.previousId() != null && !normalized.previousId().isBlank()) {
                requireSortScope(id, normalized.previousId());
                service().moveAfter(id, normalized.previousId());
                return new WebCountResponse(1);
            }
            if (normalized.nextId() != null && !normalized.nextId().isBlank()) {
                requireSortScope(id, normalized.nextId());
                service().moveBefore(id, normalized.nextId());
                return new WebCountResponse(1);
            }
            throw new IllegalArgumentException("sort requires previousId or nextId");
        });
    }

    private void requireSortScope(String id, String targetId) {
        if (!(service() instanceof DataScopeAbility<?> dataScopeAbility)) {
            return;
        }
        DataScopeAbility<?> dataScope = DataScopeAbility.cast(dataScopeAbility);
        Set<String> explicitIds = normalizeIds(id, targetId);
        DataScopeCriteriaResult scope = dataScope.requireRecordScopeResult(PlatformAction.SORT.executionPolicy(), explicitIds);
        Set<String> scopedIds = dataScope.withDataScopeTenant(scope, () -> sortScopeRecordIds(id, targetId));
        dataScope.requireRecordScopeResult(PlatformAction.SORT.executionPolicy(), scopedIds);
    }

    private Set<String> sortScopeRecordIds(String id, String targetId) {
        LinkedHashSet<String> recordIds = new LinkedHashSet<>(normalizeIds(id, targetId));
        T moving = service().select(id);
        T target = service().select(targetId);
        if (moving == null || target == null) {
            return recordIds;
        }
        service().validateSortScope(moving, target);
        service().sortedList(service().sortScope(moving)).stream()
                .map(SortCapable::getId)
                .forEach(recordIds::add);
        return java.util.Collections.unmodifiableSet(recordIds);
    }

    private Set<String> normalizeIds(String... ids) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        Arrays.stream(ids)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(normalized::add);
        return java.util.Collections.unmodifiableSet(normalized);
    }
}
