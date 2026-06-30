package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.FieldReadAbility;
import net.ximatai.muyun.spring.ability.FieldReadPolicy;
import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.ability.security.ProtectedFieldAccessor;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RecordReadProjectionPlanner {
    private static final List<String> REQUIRED_PLATFORM_FIELDS = List.of(
            StandardEntitySchema.ID_FIELD,
            StandardEntitySchema.TENANT_ID_FIELD,
            StandardEntitySchema.VERSION_FIELD
    );

    private RecordReadProjectionPlanner() {
    }

    public static RecordReadProjection defaultList(ResolvedModuleUiDescriptor descriptor,
                                                   ResolvedModuleReadModel readModel) {
        return plan(descriptor, readModel, "default_list");
    }

    public static RecordReadProjection defaultList(ResolvedModuleUiDescriptor descriptor,
                                                   ResolvedModuleReadModel readModel,
                                                   Object recordService) {
        return plan(descriptor, readModel, "default_list", recordService, null);
    }

    public static RecordReadProjection defaultList(ResolvedModuleUiDescriptor descriptor,
                                                   ResolvedModuleReadModel readModel,
                                                   Object recordService,
                                                   ActionExecutionContext actionContext) {
        return plan(descriptor, readModel, "default_list", recordService, actionContext);
    }

    public static RecordReadProjection plan(ResolvedModuleUiDescriptor descriptor,
                                            ResolvedModuleReadModel readModel,
                                            String viewCode) {
        return plan(descriptor, readModel, viewCode, null);
    }

    public static RecordReadProjection plan(ResolvedModuleUiDescriptor descriptor,
                                            ResolvedModuleReadModel readModel,
                                            String viewCode,
                                            Object recordService) {
        return plan(descriptor, readModel, viewCode, recordService, null);
    }

    public static RecordReadProjection plan(ResolvedModuleUiDescriptor descriptor,
                                            ResolvedModuleReadModel readModel,
                                            String viewCode,
                                            Object recordService,
                                            ActionExecutionContext actionContext) {
        if (descriptor == null) {
            throw new IllegalArgumentException("resolved module UI descriptor must not be null");
        }
        if (readModel == null) {
            throw new IllegalArgumentException("resolved module read model must not be null");
        }
        if (!descriptor.moduleAlias().equals(readModel.moduleAlias())) {
            throw new IllegalArgumentException("record read projection module alias mismatch: "
                    + descriptor.moduleAlias() + " != " + readModel.moduleAlias());
        }
        validateActionContext(descriptor, actionContext);
        ResolvedViewDescriptor view = view(descriptor, viewCode);
        LinkedHashSet<ViewFieldRef> outputFields = new LinkedHashSet<>();
        Set<String> readableFields = readableFields(readModel);
        FieldReadPolicy fieldReadPolicy = fieldReadPolicy(recordService, actionContext);
        for (ResolvedViewFieldDescriptor field : view.fields()) {
            if (Boolean.FALSE.equals(field.visible().constant())) {
                continue;
            }
            String fieldName = field.fieldRef().fieldName();
            if (!readableFields.contains(fieldName)) {
                throw new IllegalArgumentException("record read projection field is not readable: "
                        + descriptor.moduleAlias() + "." + view.viewCode() + "." + fieldName);
            }
            if (!fieldReadPolicy.allows(fieldName)) {
                continue;
            }
            outputFields.add(field.fieldRef());
        }
        return new RecordReadProjection(
                descriptor.moduleAlias(),
                view.viewCode(),
                actionContext == null ? null : actionContext.actionCode(),
                actionContext == null ? null : actionContext.permissionCode(),
                actionContext == null ? null : actionContext.actionPolicy().permissionActionCode(),
                fieldReadPolicies(fieldReadPolicy),
                List.copyOf(outputFields),
                REQUIRED_PLATFORM_FIELDS,
                postReadTransforms(recordService, outputFields)
        );
    }

    private static void validateActionContext(ResolvedModuleUiDescriptor descriptor,
                                              ActionExecutionContext actionContext) {
        if (actionContext == null) {
            return;
        }
        if (!descriptor.moduleAlias().equals(actionContext.moduleAlias())) {
            throw new IllegalArgumentException("record read projection action module alias mismatch: "
                    + descriptor.moduleAlias() + " != " + actionContext.moduleAlias());
        }
        if (!PlatformAction.QUERY.matches(actionContext.actionCode())) {
            throw new IllegalArgumentException("record read projection requires query action context: "
                    + descriptor.moduleAlias() + "." + actionContext.actionCode());
        }
    }

    @SuppressWarnings("rawtypes")
    private static FieldReadPolicy fieldReadPolicy(Object recordService, ActionExecutionContext actionContext) {
        if (!(recordService instanceof FieldReadAbility fieldReadAbility)) {
            return FieldReadPolicy.allReadable();
        }
        FieldReadPolicy policy = fieldReadAbility.fieldReadPolicy(actionContext);
        return policy == null ? FieldReadPolicy.allReadable() : policy;
    }

    private static List<String> fieldReadPolicies(FieldReadPolicy fieldReadPolicy) {
        if (fieldReadPolicy == null || !fieldReadPolicy.restricted()) {
            return List.of();
        }
        return List.of("fieldReadPolicy:explicit");
    }

    @SuppressWarnings("rawtypes")
    private static List<String> postReadTransforms(Object recordService, Set<ViewFieldRef> outputFields) {
        if (!(recordService instanceof FieldProtectionAbility fieldProtectionAbility)) {
            return List.of();
        }
        Set<String> outputFieldNames = outputFields.stream()
                .map(ViewFieldRef::fieldName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> transforms = new LinkedHashSet<>();
        for (Object item : fieldProtectionAbility.fieldProtectionPlan().fields()) {
            ProtectedFieldAccessor<?> field = (ProtectedFieldAccessor<?>) item;
            if (outputFieldNames.contains(field.fieldName()) && field.protection().hasOutputProtection()) {
                transforms.add(fieldProtectionTransform(field));
            }
        }
        return List.copyOf(transforms);
    }

    private static String fieldProtectionTransform(ProtectedFieldAccessor<?> field) {
        return "fieldProtection:" + field.fieldName();
    }

    private static ResolvedViewDescriptor view(ResolvedModuleUiDescriptor descriptor, String viewCode) {
        return descriptor.views().stream()
                .filter(item -> item.viewKind() == ModuleViewKind.LIST)
                .filter(item -> item.viewCode().equals(viewCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("record read projection view not found: "
                        + descriptor.moduleAlias() + "." + viewCode));
    }

    private static Set<String> readableFields(ResolvedModuleReadModel readModel) {
        LinkedHashSet<String> fields = new LinkedHashSet<>(REQUIRED_PLATFORM_FIELDS);
        readModel.fields().stream()
                .map(ResolvedModuleReadField::fieldName)
                .forEach(fields::add);
        return Set.copyOf(fields);
    }
}
