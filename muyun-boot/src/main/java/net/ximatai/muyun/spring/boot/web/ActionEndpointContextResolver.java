package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ActionEndpointContextResolver {
    static final String MODULE_ALIAS_PATH_KEY = "moduleAlias";
    static final String[] RECORD_ID_KEYS = {"id", "recordId"};
    static final String IDS_KEY = "ids";

    public Optional<ActionExecutionContext> resolve(HttpServletRequest request,
                                                    HandlerMethod handlerMethod,
                                                    ActionEndpoint endpoint) {
        String moduleAlias = moduleAlias(request, handlerMethod);
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ActionExecutionContext.ofPlatformAction(
                moduleAlias,
                endpoint.value(),
                recordIds(request),
                CurrentUserContext.currentUser()
        ));
    }

    private String moduleAlias(HttpServletRequest request, HandlerMethod handlerMethod) {
        String pathModuleAlias = pathVariable(request, MODULE_ALIAS_PATH_KEY);
        if (pathModuleAlias != null && !pathModuleAlias.isBlank()) {
            return pathModuleAlias;
        }
        Object bean = handlerMethod.getBean();
        if (bean instanceof ScopedWeb<?> scopedWeb) {
            return scopedWeb.webScopeName();
        }
        return null;
    }

    private Set<String> recordIds(HttpServletRequest request) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String key : RECORD_ID_KEYS) {
            collect(ids, pathVariable(request, key));
            collect(ids, request.getParameterValues(key));
        }
        collect(ids, pathVariable(request, IDS_KEY));
        collect(ids, request.getParameterValues(IDS_KEY));
        return Set.copyOf(ids);
    }

    @SuppressWarnings("unchecked")
    private String pathVariable(HttpServletRequest request, String key) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) {
            return null;
        }
        Object value = variables.get(key);
        return value == null ? null : value.toString();
    }

    private void collect(Set<String> ids, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .forEach(ids::add);
    }

    private void collect(Set<String> ids, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        Arrays.stream(values).forEach(value -> collect(ids, value));
    }
}
