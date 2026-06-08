package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
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

    private final PlatformModuleActionService moduleActionService;

    public ActionEndpointContextResolver() {
        this(null);
    }

    public ActionEndpointContextResolver(PlatformModuleActionService moduleActionService) {
        this.moduleActionService = moduleActionService;
    }

    public Optional<ActionExecutionContext> resolve(HttpServletRequest request,
                                                    HandlerMethod handlerMethod,
                                                    ActionEndpoint endpoint) {
        String moduleAlias = moduleAlias(request, handlerMethod);
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        ActionExecutionPolicy policy = registeredPolicy(moduleAlias, endpoint.value().code())
                .orElseGet(endpoint.value()::executionPolicy);
        return Optional.of(ActionExecutionContext.ofPolicy(
                moduleAlias,
                policy,
                recordIds(request),
                CurrentUserContext.currentUser()
        ));
    }

    public Optional<ActionExecutionContext> resolve(HttpServletRequest request,
                                                    HandlerMethod handlerMethod,
                                                    CustomActionEndpoint endpoint) {
        String moduleAlias = moduleAlias(request, handlerMethod);
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        String actionCode = PlatformNameRules.requireActionCode(endpoint.value(), "actionCode");
        ActionExecutionPolicy policy = registeredPolicy(moduleAlias, actionCode)
                .orElseGet(() -> new ActionExecutionPolicy(
                        actionCode,
                        endpoint.level(),
                        ActionAccessMode.AUTH_REQUIRED,
                        true,
                        endpoint.dataAuth(),
                        ActionDefaultGrantPolicy.NONE,
                        null
                ));
        return Optional.of(ActionExecutionContext.ofPolicy(
                moduleAlias,
                policy,
                customRecordIds(request, endpoint),
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
        PlatformStaticModule staticModule = handlerMethod.getBeanType().getAnnotation(PlatformStaticModule.class);
        if (staticModule != null && !staticModule.alias().isBlank()) {
            return staticModule.alias();
        }
        return null;
    }

    private Optional<ActionExecutionPolicy> registeredPolicy(String moduleAlias, String actionCode) {
        if (moduleActionService == null) {
            return Optional.empty();
        }
        PlatformModuleAction action = moduleActionService.findByModuleAliasAndActionCode(moduleAlias, actionCode);
        if (action == null || Boolean.FALSE.equals(action.getEnabled())) {
            return Optional.empty();
        }
        return Optional.of(toPolicy(action));
    }

    private ActionExecutionPolicy toPolicy(PlatformModuleAction action) {
        String actionCode = PlatformNameRules.requireActionCode(action.getActionCode(), "actionCode");
        String permissionActionCode = action.getPermissionActionCode();
        String inheritActionCode = permissionActionCode == null || permissionActionCode.isBlank()
                || permissionActionCode.equals(actionCode)
                ? null
                : PlatformNameRules.requireActionCode(permissionActionCode, "permissionActionCode");
        return new ActionExecutionPolicy(
                actionCode,
                toPlatformLevel(action.getActionLevel()),
                action.getAccessMode() == null
                        ? ActionAccessMode.AUTH_REQUIRED
                        : ActionAccessMode.valueOf(action.getAccessMode().name()),
                action.getActionAuth() == null || Boolean.TRUE.equals(action.getActionAuth()),
                Boolean.TRUE.equals(action.getDataAuth()),
                action.getDefaultGrantPolicy(),
                inheritActionCode
        );
    }

    private PlatformActionLevel toPlatformLevel(EntityActionLevel level) {
        if (level == null) {
            return PlatformActionLevel.ANY;
        }
        return switch (level) {
            case LIST -> PlatformActionLevel.LIST;
            case RECORD -> PlatformActionLevel.RECORD;
            case BATCH -> PlatformActionLevel.BATCH;
            case ANY -> PlatformActionLevel.ANY;
        };
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

    private Set<String> customRecordIds(HttpServletRequest request, CustomActionEndpoint endpoint) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(recordIds(request));
        String key = endpoint.recordIdPathVariable();
        if (key != null && !key.isBlank()) {
            collect(ids, pathVariable(request, key));
        }
        return Set.copyOf(ids);
    }

    private String pathVariable(HttpServletRequest request, String key) {
        Object value = pathVariables(request).get(key);
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> pathVariables(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) {
            return Map.of();
        }
        return variables.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));
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
