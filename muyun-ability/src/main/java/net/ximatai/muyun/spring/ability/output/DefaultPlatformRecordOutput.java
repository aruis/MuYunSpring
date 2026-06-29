package net.ximatai.muyun.spring.ability.output;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;
import java.util.Objects;

public class DefaultPlatformRecordOutput implements PlatformRecordOutput {
    private final List<RecordOutputTransformer> transformers;

    public DefaultPlatformRecordOutput(List<RecordOutputTransformer> transformers) {
        this.transformers = transformers == null ? List.of() : List.copyOf(transformers);
    }

    @Override
    public <T extends EntityContract> T record(CrudAbility<T> service, T record, RecordOutputContext context) {
        Objects.requireNonNull(service, "service must not be null");
        if (record == null) {
            return null;
        }
        T transformed = record;
        RecordOutputContext normalized = normalize(context);
        for (RecordOutputTransformer transformer : transformers) {
            if (transformer.supports(service, normalized)) {
                transformed = transformer.transformRecord(service, transformed, normalized);
            }
        }
        return transformed;
    }

    @Override
    public <T extends EntityContract> List<T> records(CrudAbility<T> service,
                                                      List<T> records,
                                                      RecordOutputContext context) {
        Objects.requireNonNull(service, "service must not be null");
        if (records == null || records.isEmpty()) {
            return records;
        }
        List<T> transformed = records;
        RecordOutputContext normalized = normalize(context);
        for (RecordOutputTransformer transformer : transformers) {
            if (transformer.supports(service, normalized)) {
                transformed = transformer.transformRecords(service, transformed, normalized);
            }
        }
        return transformed;
    }

    private RecordOutputContext normalize(RecordOutputContext context) {
        return context == null ? RecordOutputContext.view() : context;
    }
}
