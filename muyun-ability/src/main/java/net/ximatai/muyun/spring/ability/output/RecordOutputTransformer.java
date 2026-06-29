package net.ximatai.muyun.spring.ability.output;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

public interface RecordOutputTransformer {
    default boolean supports(CrudAbility<?> service, RecordOutputContext context) {
        return true;
    }

    default <T extends EntityContract> T transformRecord(CrudAbility<T> service,
                                                         T record,
                                                         RecordOutputContext context) {
        return record;
    }

    default <T extends EntityContract> List<T> transformRecords(CrudAbility<T> service,
                                                                List<T> records,
                                                                RecordOutputContext context) {
        if (records == null || records.isEmpty()) {
            return records;
        }
        return records.stream()
                .map(record -> transformRecord(service, record, context))
                .toList();
    }
}
