package net.ximatai.muyun.spring.ability.output;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;

import java.util.List;

public interface PlatformRecordOutput {
    <T extends EntityContract> T record(CrudAbility<T> service, T record, RecordOutputContext context);

    <T extends EntityContract> List<T> records(CrudAbility<T> service, List<T> records, RecordOutputContext context);

    default <T extends EntityContract> PageResult<T> page(CrudAbility<T> service,
                                                          PageResult<T> page,
                                                          RecordOutputContext context) {
        if (page == null) {
            return null;
        }
        List<T> records = records(service, page.getRecords(), context);
        return PageResult.of(records, page.getTotal(), PageRequest.of(page.getPageNum(), page.getPageSize()));
    }

    default <T extends EntityContract> T record(CrudAbility<T> service, T record, FieldOutputContext context) {
        return record(service, record, RecordOutputContext.of(context));
    }

    default <T extends EntityContract> List<T> records(CrudAbility<T> service,
                                                       List<T> records,
                                                       FieldOutputContext context) {
        return records(service, records, RecordOutputContext.of(context));
    }

    default <T extends EntityContract> PageResult<T> page(CrudAbility<T> service,
                                                          PageResult<T> page,
                                                          FieldOutputContext context) {
        return page(service, page, RecordOutputContext.of(context));
    }
}
