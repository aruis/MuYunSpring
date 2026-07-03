package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedMap;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticActionContribution;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticActionContributionSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
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

    public PlatformModuleActionService moduleActionService() {
        return moduleActionService;
    }

    public Optional<ActionExecutionContext> resolve(ContainerRequestContext requestContext, ResourceInfo resourceInfo) {
        if (resourceInfo == null || resourceInfo.getResourceMethod() == null) {
            return Optional.empty();
        }
        Method method = resourceInfo.getResourceMethod();
        ActionEndpoint endpoint = method.getAnnotation(ActionEndpoint.class);
        if (endpoint != null) {
            return resolve(requestContext, resourceInfo, endpoint);
        }
        CustomActionEndpoint customEndpoint = method.getAnnotation(CustomActionEndpoint.class);
        if (customEndpoint != null) {
            return resolve(requestContext, resourceInfo, customEndpoint);
        }
        return Optional.empty();
    }

    public Optional<ActionExecutionContext> resolve(ContainerRequestContext requestContext,
                                                    ResourceInfo resourceInfo,
                                                    ActionEndpoint endpoint) {
        PlatformStaticActionContribution contribution = contribution(resourceInfo);
        String moduleAlias = contribution == null
                ? moduleAlias(requestContext, resourceInfo)
                : PlatformStaticActionContributionSupport.targetModule(contribution);
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        String actionCode = contribution == null
                ? endpoint.value().code()
                : PlatformStaticActionContributionSupport.actionCode(contribution, endpoint.value());
        ActionExecutionPolicy policy = registeredPolicy(moduleAlias, actionCode)
                .orElseGet(() -> contribution == null
                        ? endpoint.value().executionPolicy()
                        : contributionPolicy(contribution, endpoint.value()));
        return Optional.of(ActionExecutionContext.ofPolicy(
                moduleAlias,
                policy,
                recordIds(requestContext),
                CurrentUserContext.currentUser()
        ));
    }

    public Optional<ActionExecutionContext> resolve(ContainerRequestContext requestContext,
                                                    ResourceInfo resourceInfo,
                                                    CustomActionEndpoint endpoint) {
        PlatformStaticActionContribution contribution = contribution(resourceInfo);
        String moduleAlias = contribution == null
                ? moduleAlias(requestContext, resourceInfo)
                : PlatformStaticActionContributionSupport.targetModule(contribution);
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        String actionCode = contribution == null
                ? PlatformNameRules.requireActionCode(endpoint.value(), "actionCode")
                : PlatformStaticActionContributionSupport.actionCode(contribution, endpoint.value());
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
                customRecordIds(requestContext, endpoint),
                CurrentUserContext.currentUser()
        ));
    }

    private PlatformStaticActionContribution contribution(ResourceInfo resourceInfo) {
        PlatformStaticActionContribution methodContribution =
                resourceInfo.getResourceMethod().getAnnotation(PlatformStaticActionContribution.class);
        if (methodContribution != null) {
            return methodContribution;
        }
        Class<?> resourceClass = resourceInfo.getResourceClass();
        return resourceClass == null ? null : resourceClass.getAnnotation(PlatformStaticActionContribution.class);
    }

    private ActionExecutionPolicy contributionPolicy(PlatformStaticActionContribution contribution,
                                                     net.ximatai.muyun.spring.common.platform.PlatformAction action) {
        String actionCode = PlatformStaticActionContributionSupport.actionCode(contribution, action);
        String permissionActionCode = PlatformStaticActionContributionSupport.permissionActionCode(contribution, action);
        return new ActionExecutionPolicy(
                actionCode,
                action.level(),
                action.accessMode(),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                actionCode.equals(permissionActionCode) ? null : permissionActionCode
        );
    }

    private String moduleAlias(ContainerRequestContext requestContext, ResourceInfo resourceInfo) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        PlatformStaticModule staticModule = resourceClass == null
                ? null
                : resourceClass.getAnnotation(PlatformStaticModule.class);
        if (staticModule != null && !staticModule.alias().isBlank()) {
            return staticModule.alias();
        }
        return pathVariable(requestContext, MODULE_ALIAS_PATH_KEY);
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

    private Set<String> recordIds(ContainerRequestContext requestContext) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String key : RECORD_ID_KEYS) {
            collect(ids, pathVariable(requestContext, key));
            collect(ids, queryValues(requestContext, key));
        }
        collect(ids, pathVariable(requestContext, IDS_KEY));
        collect(ids, queryValues(requestContext, IDS_KEY));
        return Set.copyOf(ids);
    }

    private Set<String> customRecordIds(ContainerRequestContext requestContext, CustomActionEndpoint endpoint) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(recordIds(requestContext));
        String key = endpoint.recordIdPathVariable();
        if (key != null && !key.isBlank()) {
            collect(ids, pathVariable(requestContext, key));
        }
        return Set.copyOf(ids);
    }

    private String pathVariable(ContainerRequestContext requestContext, String key) {
        if (requestContext == null || requestContext.getUriInfo() == null) {
            return null;
        }
        return first(requestContext.getUriInfo().getPathParameters(false), key);
    }

    private java.util.List<String> queryValues(ContainerRequestContext requestContext, String key) {
        if (requestContext == null || requestContext.getUriInfo() == null) {
            return java.util.List.of();
        }
        return requestContext.getUriInfo().getQueryParameters(false).getOrDefault(key, java.util.List.of());
    }

    private String first(MultivaluedMap<String, String> values, String key) {
        if (values == null) {
            return null;
        }
        return values.getFirst(key);
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

    private void collect(Set<String> ids, Iterable<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            collect(ids, value);
        }
    }
}
