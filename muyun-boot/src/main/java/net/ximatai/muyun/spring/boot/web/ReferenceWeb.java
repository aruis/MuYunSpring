package net.ximatai.muyun.spring.boot.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReferenceWeb<S, Q, R> extends ScopedWeb<S> {
    R resolveReference(String fieldName, Q request);

    @PostMapping("/references/{fieldName}/resolve")
    default R reference(@PathVariable String fieldName,
                        @RequestBody(required = false) Q request) {
        return webScope(() -> resolveReference(fieldName, request));
    }
}
