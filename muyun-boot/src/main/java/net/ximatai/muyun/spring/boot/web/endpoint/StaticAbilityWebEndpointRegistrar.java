package net.ximatai.muyun.spring.boot.web.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticActionContribution;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticActionContributionSupport;
import net.ximatai.muyun.spring.boot.platform.StaticServiceAbilityCompiler;
import net.ximatai.muyun.spring.boot.web.ScopedWeb;
import net.ximatai.muyun.spring.boot.web.StaticAbilityOperationRuntime;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.deletion.RecycleBinFacade;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Compiles service abilities into real Spring MVC mappings at application startup. */
public class StaticAbilityWebEndpointRegistrar implements SmartInitializingSingleton {
    private final ApplicationContext applicationContext;
    private final RequestMappingHandlerMapping handlerMapping;
    private final RegisteredWebEndpointCatalog endpointCatalog;
    private final ObjectProvider<RecycleBinFacade> recycleBinFacade;
    private final PlatformWebOperationDispatcher dispatcher;

    public StaticAbilityWebEndpointRegistrar(ApplicationContext applicationContext,
                                             RequestMappingHandlerMapping handlerMapping,
                                             RegisteredWebEndpointCatalog endpointCatalog,
                                             ObjectProvider<RecycleBinFacade> recycleBinFacade) {
        this(applicationContext, handlerMapping, endpointCatalog, recycleBinFacade, new ObjectMapper());
    }

    public StaticAbilityWebEndpointRegistrar(ApplicationContext applicationContext,
                                             RequestMappingHandlerMapping handlerMapping,
                                             RegisteredWebEndpointCatalog endpointCatalog,
                                             ObjectProvider<RecycleBinFacade> recycleBinFacade,
                                             ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.handlerMapping = handlerMapping;
        this.endpointCatalog = endpointCatalog;
        this.recycleBinFacade = recycleBinFacade;
        this.dispatcher = new PlatformWebOperationDispatcher(endpointCatalog, objectMapper,
                new StaticAbilityOperationRuntime(recycleBinFacade));
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : applicationContext.getBeanNamesForAnnotation(PlatformStaticModule.class)) {
            Object bean = applicationContext.getBean(beanName);
            if (!(bean instanceof ScopedWeb<?> anchor)) {
                continue;
            }
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            PlatformStaticModule module = AnnotationUtils.findAnnotation(beanClass, PlatformStaticModule.class);
            if (module == null) {
                continue;
            }
            List<String> basePaths = basePaths(beanClass, module.alias());
            Object service = anchor.service();
            contribute(module.alias(), basePaths, anchor, service, null);
        }
        for (String beanName : applicationContext.getBeanNamesForAnnotation(PlatformStaticActionContribution.class)) {
            Object bean = applicationContext.getBean(beanName);
            if (!(bean instanceof ScopedWeb<?> anchor)) {
                continue;
            }
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            PlatformStaticActionContribution contribution =
                    AnnotationUtils.findAnnotation(beanClass, PlatformStaticActionContribution.class);
            if (contribution == null) {
                continue;
            }
            String moduleAlias = PlatformStaticActionContributionSupport.targetModule(contribution);
            contribute(moduleAlias, basePaths(beanClass, moduleAlias), anchor, anchor.service(), contribution);
        }
        registerExplicitControllerEndpoints();
    }

    private void contribute(String moduleAlias,
                            List<String> basePaths,
                            ScopedWeb<?> anchor,
                            Object service,
                            PlatformStaticActionContribution contribution) {
        contributeEnable(moduleAlias, basePaths, anchor, service, contribution);
        contributeTree(moduleAlias, basePaths, anchor, service, contribution);
        contributeSort(moduleAlias, basePaths, anchor, service, contribution);
        contributeRecycleBin(moduleAlias, basePaths, anchor, service, contribution);
    }

    private void contributeTree(String moduleAlias,
                                List<String> basePaths,
                                ScopedWeb<?> anchor,
                                Object service,
                                PlatformStaticActionContribution contribution) {
        if (!(service instanceof TreeAbility<?> ability)) {
            return;
        }
        StaticWebOperationTarget target = new StaticWebOperationTarget(moduleAlias, anchor, ability);
        if (enabled(service, PlatformAction.TREE)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "tree"), "tree",
                    PlatformAction.TREE, RequestMethod.GET, "/tree", contribution));
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "tree"), "subtree",
                    PlatformAction.TREE, RequestMethod.GET, "/tree/{id}", contribution));
        }
        if (enabled(service, PlatformAction.SORT)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "tree"), "sort",
                    PlatformAction.SORT, RequestMethod.POST, "/sort/{id}", contribution));
        }
    }

    private void contributeEnable(String moduleAlias,
                                  List<String> basePaths,
                                  ScopedWeb<?> anchor,
                                  Object service,
                                  PlatformStaticActionContribution contribution) {
        if (!(service instanceof EnableAbility<?> ability)) {
            return;
        }
        StaticWebOperationTarget target = new StaticWebOperationTarget(moduleAlias, anchor, ability);
        if (directOperation(service, PlatformAction.ENABLE)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "enable"), "enable",
                    PlatformAction.ENABLE, RequestMethod.POST, "/enable/{id}", contribution));
        }
        if (directOperation(service, PlatformAction.DISABLE)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "enable"), "disable",
                    PlatformAction.DISABLE, RequestMethod.POST, "/disable/{id}", contribution));
        }
    }

    private void contributeSort(String moduleAlias,
                                List<String> basePaths,
                                ScopedWeb<?> anchor,
                                Object service,
                                PlatformStaticActionContribution contribution) {
        if (!(service instanceof SortAbility<?> ability)
                || service instanceof TreeAbility<?>
                || !enabled(service, PlatformAction.SORT)) {
            return;
        }
        StaticWebOperationTarget target = new StaticWebOperationTarget(moduleAlias, anchor, ability);
        register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "sort"), "sort",
                PlatformAction.SORT, RequestMethod.POST, "/sort/{id}", contribution));
    }

    private void contributeRecycleBin(String moduleAlias,
                                      List<String> basePaths,
                                      ScopedWeb<?> anchor,
                                      Object service,
                                      PlatformStaticActionContribution contribution) {
        if (!(service instanceof RecycleBinAbility<?> ability)) {
            return;
        }
        RecycleBinFacade facade = recycleBinFacade.getIfAvailable();
        if (facade == null) {
            throw new IllegalStateException("RecycleBinFacade is required by " + moduleAlias + ".recycleBin");
        }
        StaticWebOperationTarget target = new StaticWebOperationTarget(moduleAlias, anchor, ability);
        if (enabled(service, PlatformAction.RECYCLE_BIN_QUERY)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "recycleBin"), "query",
                    PlatformAction.RECYCLE_BIN_QUERY, RequestMethod.POST, "/recycle-bin/query", contribution));
        }
        if (enabled(service, PlatformAction.RECYCLE_BIN_RESTORE)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "recycleBin"), "restore",
                    PlatformAction.RECYCLE_BIN_RESTORE, RequestMethod.POST,
                    "/recycle-bin/{sourceDeleteOperationId}/restore", contribution));
        }
        if (ability.isRecycleBinPurgeEnabled() && enabled(service, PlatformAction.RECYCLE_BIN_PURGE)) {
            register(basePaths, target, operation(moduleAlias, abilityCode(contribution, "recycleBin"), "purge",
                    PlatformAction.RECYCLE_BIN_PURGE, RequestMethod.POST,
                    "/recycle-bin/{sourceDeleteOperationId}/purge", contribution));
        }
    }

    private boolean directOperation(Object service, PlatformAction action) {
        return StaticServiceAbilityCompiler.operationMethods(service).containsKey(action);
    }

    private boolean enabled(Object service, PlatformAction action) {
        return !StaticServiceAbilityCompiler.disabledActions(service).contains(action);
    }

    private void register(List<String> basePaths,
                          StaticWebOperationTarget target,
                          EndpointOperation operation) {
        for (int index = 0; index < basePaths.size(); index++) {
            ResolvedWebEndpoint template = operation.definition();
            String fullPath = join(basePaths.get(index), template.path());
            String endpointId = index == 0 ? template.endpointId() : template.endpointId() + "@" + index;
            ResolvedWebEndpoint definition = new ResolvedWebEndpoint(
                    endpointId, template.moduleAlias(), template.abilityCode(), template.operationCode(),
                    template.action(), template.method(), fullPath, template.source(), template.executionPolicy());
            RequestMappingInfo mapping = RequestMappingInfo.paths(fullPath)
                    .methods(template.method())
                    .options(handlerMapping.getBuilderConfiguration())
                    .build();
            ExistingMapping existing = findExistingMapping(fullPath, template.method());
            if (existing != null) {
                throw new IllegalStateException("explicit controller cannot replace enabled standard ability endpoint "
                        + definition.method() + " " + definition.path()
                        + "; disable " + definition.action().code() + " on the concrete service first: "
                        + existing.handler());
            }
            try {
                handlerMapping.registerMapping(mapping, dispatcher, dispatcher.handlerMethod());
            } catch (RuntimeException failure) {
                throw new IllegalStateException("failed to register ability endpoint " + endpointId
                        + " at " + template.method() + " " + fullPath, failure);
            }
            endpointCatalog.register(new RegisteredWebEndpoint(
                    definition, mapping, dispatcher, dispatcher.handlerMethod(), target));
        }
    }

    private ExistingMapping findExistingMapping(String path, RequestMethod method) {
        List<ExistingMapping> matches = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getPatternValues().contains(path))
                .filter(entry -> entry.getKey().getMethodsCondition().getMethods().isEmpty()
                        || entry.getKey().getMethodsCondition().getMethods().contains(method))
                .map(entry -> new ExistingMapping(entry.getKey(), entry.getValue()))
                .toList();
        if (matches.size() > 1) {
            throw new IllegalStateException("multiple explicit mappings conflict with ability endpoint "
                    + method + " " + path + ": " + matches.stream().map(ExistingMapping::handler).toList());
        }
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private void registerExplicitControllerEndpoints() {
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (endpointCatalog.contains(mapping, handler)) {
                return;
            }
            ActionEndpoint endpoint = AnnotationUtils.findAnnotation(handler.getMethod(), ActionEndpoint.class);
            String moduleAlias = explicitModuleAlias(handler.getBeanType());
            if (endpoint == null || moduleAlias == null) {
                return;
            }
            List<RequestMethod> methods = List.copyOf(mapping.getMethodsCondition().getMethods());
            if (methods.size() != 1) {
                throw new IllegalStateException("platform action endpoint requires exactly one HTTP method: "
                        + handler);
            }
            for (String path : mapping.getPatternValues()) {
                String operationCode = handler.getMethod().getName();
                String endpointId = moduleAlias + ".controller." + operationCode + "."
                        + methods.getFirst().name().toLowerCase(Locale.ROOT) + "."
                        + Integer.toUnsignedString(path.hashCode(), 36);
                ResolvedWebEndpoint definition = new ResolvedWebEndpoint(
                        endpointId,
                        moduleAlias,
                        "controller",
                        operationCode,
                        endpoint.value(),
                        methods.getFirst(),
                        path,
                        ResolvedWebEndpoint.Source.STATIC_EXPLICIT
                );
                endpointCatalog.register(new RegisteredWebEndpoint(
                        definition, mapping, handler.getBean(), handler.getMethod()));
            }
        });
    }

    private String explicitModuleAlias(Class<?> beanClass) {
        PlatformStaticModule module = AnnotationUtils.findAnnotation(beanClass, PlatformStaticModule.class);
        return module == null ? null : module.alias();
    }

    private EndpointOperation operation(String moduleAlias,
                                        String abilityCode,
                                        String operationCode,
                                        PlatformAction action,
                                        RequestMethod method,
                                        String path,
                                        PlatformStaticActionContribution contribution) {
        ResolvedWebEndpoint definition = new ResolvedWebEndpoint(
                moduleAlias + "." + abilityCode + "." + operationCode,
                moduleAlias, abilityCode, operationCode, action, method, path,
                ResolvedWebEndpoint.Source.STATIC_ABILITY,
                contribution == null ? action.executionPolicy() : contributionPolicy(contribution, action));
        return new EndpointOperation(definition);
    }

    private String abilityCode(PlatformStaticActionContribution contribution, String abilityCode) {
        return contribution == null ? abilityCode : contribution.resource() + "." + abilityCode;
    }

    private ActionExecutionPolicy contributionPolicy(PlatformStaticActionContribution contribution,
                                                       PlatformAction action) {
        String actionCode = PlatformStaticActionContributionSupport.actionCode(contribution, action);
        String permissionActionCode = PlatformStaticActionContributionSupport.permissionActionCode(
                contribution, action);
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

    private List<String> basePaths(Class<?> beanClass, String moduleAlias) {
        RequestMapping mapping = AnnotationUtils.findAnnotation(beanClass, RequestMapping.class);
        if (mapping == null) {
            return List.of("/" + moduleAlias);
        }
        String[] values = mapping.path().length == 0 ? mapping.value() : mapping.path();
        if (values.length == 0) {
            return List.of("/" + moduleAlias);
        }
        return Arrays.stream(values).map(this::normalizePath).toList();
    }

    private String join(String basePath, String endpointPath) {
        return normalizePath(basePath) + normalizePath(endpointPath);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private record EndpointOperation(ResolvedWebEndpoint definition) {
    }

    private record ExistingMapping(RequestMappingInfo mapping,
                                   org.springframework.web.method.HandlerMethod handler) {
    }
}
