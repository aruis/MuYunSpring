package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReferenceWeb<S, Q, R> extends ScopedWeb<S> {
    R resolveReference(String fieldName, Q request);

    @PostMapping("/references/{fieldName}/resolve")
    @ActionEndpoint(PlatformAction.REFERENCE)
    default R reference(@PathVariable String fieldName,
                        @RequestBody(required = false) Q request) {
        return webScope(() -> resolveReference(fieldName, request));
    }
}
