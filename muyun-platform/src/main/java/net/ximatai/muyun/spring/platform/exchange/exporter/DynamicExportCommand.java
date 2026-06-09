package net.ximatai.muyun.spring.platform.exchange.exporter;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;

import java.util.List;

public record DynamicExportCommand(
        DynamicModuleDescriptor descriptor,
        Criteria criteria,
        PageRequest pageRequest,
        List<Sort> sorts
) {
    public DynamicExportCommand {
        if (descriptor == null) {
            throw new PlatformException("dynamic export requires module descriptor");
        }
        criteria = criteria == null ? Criteria.of() : criteria;
        pageRequest = pageRequest == null ? PageRequest.of(1, 500) : pageRequest;
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public Sort[] sortArray() {
        return sorts.toArray(Sort[]::new);
    }
}
