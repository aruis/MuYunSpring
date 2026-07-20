package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

/** Resolves options through a module field, keeping the frontend independent of source implementations. */
@Service
public class ModuleFieldOptionService {
    private final StaticModuleDefinitionCatalog staticModuleCatalog;
    private final DynamicRecordService dynamicRecordService;
    private final OptionSourceRegistry optionSourceRegistry;

    public ModuleFieldOptionService(StaticModuleDefinitionCatalog staticModuleCatalog,
                                    ObjectProvider<DynamicRecordService> dynamicRecordService,
                                    OptionSourceRegistry optionSourceRegistry) {
        this.staticModuleCatalog = staticModuleCatalog;
        this.dynamicRecordService = dynamicRecordService == null ? null : dynamicRecordService.getIfAvailable();
        this.optionSourceRegistry = optionSourceRegistry;
    }

    public List<OptionItem> options(String moduleAlias, String fieldName, boolean enabledOnly, String parentCode) {
        OptionBinding binding = binding(moduleAlias, fieldName);
        return optionSourceRegistry.source(binding).options(new OptionQuery(enabledOnly, parentCode));
    }

    private OptionBinding binding(String moduleAlias, String fieldName) {
        return staticModuleCatalog.find(moduleAlias)
                .flatMap(definition -> OptionFieldResolver.resolve(definition.modelClass()).stream()
                        .filter(field -> field.fieldName().equals(fieldName))
                        .findFirst()
                        .map(field -> field.binding()))
                .orElseGet(() -> dynamicBinding(moduleAlias, fieldName));
    }

    private OptionBinding dynamicBinding(String moduleAlias, String fieldName) {
        if (dynamicRecordService != null) {
            DynamicModuleDescriptor module = dynamicRecordService.describe(moduleAlias);
            OptionBinding binding = module.entities().stream()
                    .filter(entity -> entity.entityAlias().equals(module.mainEntityAlias()))
                    .flatMap(entity -> entity.fields().stream())
                    .filter(field -> field.fieldName().equals(fieldName))
                    .map(field -> field.optionBinding())
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (binding != null) {
                return binding;
            }
        }
        throw new PlatformException(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404,
                "option field not found: " + moduleAlias + "." + fieldName);
    }
}
