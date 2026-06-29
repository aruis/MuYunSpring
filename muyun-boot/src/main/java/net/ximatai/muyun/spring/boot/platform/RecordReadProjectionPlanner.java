package net.ximatai.muyun.spring.boot.platform;

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

    public static RecordReadProjection plan(ResolvedModuleUiDescriptor descriptor,
                                            ResolvedModuleReadModel readModel,
                                            String viewCode) {
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
        ResolvedViewDescriptor view = view(descriptor, viewCode);
        LinkedHashSet<ViewFieldRef> outputFields = new LinkedHashSet<>();
        Set<String> readableFields = readableFields(readModel);
        for (ResolvedViewFieldDescriptor field : view.fields()) {
            if (Boolean.FALSE.equals(field.visible().constant())) {
                continue;
            }
            String fieldName = field.fieldRef().fieldName();
            if (!readableFields.contains(fieldName)) {
                throw new IllegalArgumentException("record read projection field is not readable: "
                        + descriptor.moduleAlias() + "." + view.viewCode() + "." + fieldName);
            }
            outputFields.add(field.fieldRef());
        }
        return new RecordReadProjection(
                descriptor.moduleAlias(),
                view.viewCode(),
                List.copyOf(outputFields),
                REQUIRED_PLATFORM_FIELDS,
                List.of()
        );
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
