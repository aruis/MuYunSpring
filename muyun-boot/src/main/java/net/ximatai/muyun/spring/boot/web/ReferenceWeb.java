package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

public interface ReferenceWeb<S, Q, R> extends ScopedWeb<S> {
    R resolveReference(String fieldName, Q request);

    @POST
    @Path("/references/{fieldName}/resolve")
    @ActionEndpoint(PlatformAction.REFERENCE)
    default R reference(@PathParam("fieldName") String fieldName,
                        Q request) {
        return webScope(() -> resolveReference(fieldName, request));
    }
}
