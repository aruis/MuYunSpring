package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.web.PlatformWebPathRules;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;

import java.util.List;

public interface ActionWeb<S, Q, D, A, R> extends ScopedWeb<S> {
    List<D> listActions();

    List<A> listRecordActions(String recordId);

    R executeListAction(String actionCode, Q request);

    R executeBatchAction(String actionCode, Q request);

    R executeRecordAction(String actionCode, String recordId, Q request);

    @GET
    @Path("/actions")
    default List<D> actions() {
        return webScope(this::listActions);
    }

    @GET
    @Path("/actions/{recordId}")
    default List<A> recordActions(@PathParam("recordId") String recordId) {
        return webScope(() -> listRecordActions(recordId));
    }

    @POST
    @Path("/" + PlatformWebPathRules.ACTION_CODE_PATH)
    default R listAction(@PathParam("actionCode") String actionCode,
                         Q request) {
        return webScope(() -> executeListAction(actionCode, request));
    }

    @POST
    @Path("/" + PlatformWebPathRules.ACTION_CODE_PATH + "/batch")
    default R batchAction(@PathParam("actionCode") String actionCode,
                          Q request) {
        return webScope(() -> executeBatchAction(actionCode, request));
    }

    @POST
    @Path("/" + PlatformWebPathRules.ACTION_CODE_PATH + "/{recordId}")
    default R recordAction(@PathParam("actionCode") String actionCode,
                           @PathParam("recordId") String recordId,
                           Q request) {
        return webScope(() -> executeRecordAction(actionCode, recordId, request));
    }
}
