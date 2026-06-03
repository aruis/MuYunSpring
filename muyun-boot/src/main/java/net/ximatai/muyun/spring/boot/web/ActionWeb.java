package net.ximatai.muyun.spring.boot.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface ActionWeb<S, Q, D, A, R> extends ScopedWeb<S> {
    List<D> listActions();

    List<A> listRecordActions(String recordId);

    R executeListAction(String actionCode, Q request);

    R executeBatchAction(String actionCode, Q request);

    R executeRecordAction(String actionCode, String recordId, Q request);

    @PostMapping("/actions")
    default List<D> actions() {
        return webScope(this::listActions);
    }

    @PostMapping("/actions/{recordId}")
    default List<A> recordActions(@PathVariable String recordId) {
        return webScope(() -> listRecordActions(recordId));
    }

    @PostMapping("/{actionCode}")
    default R listAction(@PathVariable String actionCode,
                         @RequestBody(required = false) Q request) {
        return webScope(() -> executeListAction(actionCode, request));
    }

    @PostMapping("/{actionCode}/batch")
    default R batchAction(@PathVariable String actionCode,
                          @RequestBody(required = false) Q request) {
        return webScope(() -> executeBatchAction(actionCode, request));
    }

    @PostMapping("/{actionCode}/{recordId}")
    default R recordAction(@PathVariable String actionCode,
                           @PathVariable String recordId,
                           @RequestBody(required = false) Q request) {
        return webScope(() -> executeRecordAction(actionCode, recordId, request));
    }
}
