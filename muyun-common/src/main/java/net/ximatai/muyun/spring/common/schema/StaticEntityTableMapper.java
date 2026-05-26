package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.annotation.AnnotationProcessor;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.model.StandardEntity;

import java.util.Objects;

public class StaticEntityTableMapper {
    private final PlatformTableValidator tableValidator;

    public StaticEntityTableMapper() {
        this(new PlatformTableValidator());
    }

    public StaticEntityTableMapper(PlatformTableValidator tableValidator) {
        this.tableValidator = Objects.requireNonNull(tableValidator, "tableValidator must not be null");
    }

    public TableWrapper toTable(Class<?> modelClass) {
        requirePlatformEntity(modelClass);
        TableWrapper table = AnnotationProcessor.fromEntityClass(modelClass);
        tableValidator.requireStandardEntityTable(table, modelClass.getName());
        return table;
    }

    private void requirePlatformEntity(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        if (!StandardEntity.class.isAssignableFrom(modelClass)) {
            throw new IllegalArgumentException("static entity must extend StandardEntity: " + modelClass.getName());
        }
    }
}
