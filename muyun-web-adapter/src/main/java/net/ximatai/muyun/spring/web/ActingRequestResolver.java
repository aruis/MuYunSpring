package net.ximatai.muyun.spring.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.identity.ActingContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;

import java.util.Optional;

/** Resolves an optional domain-specific acting identity for an HTTP action. */
public interface ActingRequestResolver {
    Optional<ActingContext> resolve(HttpServletRequest request, ActionExecutionContext actionContext);
}
