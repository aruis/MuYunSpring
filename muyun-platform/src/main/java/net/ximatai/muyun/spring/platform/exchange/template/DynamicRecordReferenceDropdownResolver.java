package net.ximatai.muyun.spring.platform.exchange.template;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class DynamicRecordReferenceDropdownResolver implements DynamicReferenceDropdownResolver {
    private final DynamicRecordService recordService;

    public DynamicRecordReferenceDropdownResolver(DynamicRecordService recordService) {
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
    }

    @Override
    public List<String> resolve(DynamicReferenceDescriptor reference, int limit) {
        if (reference == null || limit <= 0) {
            return List.of();
        }
        var options = recordService.referenceOptions(
                reference.targetModuleAlias(),
                reference.targetEntityAlias(),
                Criteria.of(),
                PageRequest.of(1, limit + 1)
        );
        if (options.getTotal() > limit || options.getRecords().size() > limit) {
            return List.of();
        }
        List<String> titles = new ArrayList<>();
        Set<String> uniqueTitles = new LinkedHashSet<>();
        for (ReferenceOption option : options.getRecords()) {
            String title = option.title();
            if (title == null || title.isBlank()) {
                continue;
            }
            if (!uniqueTitles.add(title)) {
                return List.of();
            }
            titles.add(title);
        }
        return List.copyOf(titles);
    }
}
