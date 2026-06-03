package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface SortWeb<T extends SortCapable, S extends SortAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/sort/{id}")
    default WebCountResponse sort(@PathVariable String id,
                                  @RequestBody(required = false) SortWebRequest request) {
        return webScope(() -> {
            SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
            if (normalized.previousId() != null && !normalized.previousId().isBlank()) {
                service().moveAfter(id, normalized.previousId());
                return new WebCountResponse(1);
            }
            if (normalized.nextId() != null && !normalized.nextId().isBlank()) {
                service().moveBefore(id, normalized.nextId());
                return new WebCountResponse(1);
            }
            throw new IllegalArgumentException("sort requires previousId or nextId");
        });
    }
}
