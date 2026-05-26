package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.annotation.AnnotationProcessor;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.model.StandardBaseModel;

import java.util.Objects;

public class StaticModelTableMapper {
    private final PlatformTableValidator tableValidator;

    public StaticModelTableMapper() {
        this(new PlatformTableValidator());
    }

    public StaticModelTableMapper(PlatformTableValidator tableValidator) {
        this.tableValidator = Objects.requireNonNull(tableValidator, "tableValidator must not be null");
    }

    public TableWrapper toTable(Class<?> modelClass) {
        requirePlatformModel(modelClass);
        TableWrapper table = AnnotationProcessor.fromEntityClass(modelClass);
        tableValidator.requireStandardModelTable(table, modelClass.getName());
        return table;
    }

    private void requirePlatformModel(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        if (!StandardBaseModel.class.isAssignableFrom(modelClass)) {
            throw new IllegalArgumentException("static model must extend StandardBaseModel: " + modelClass.getName());
        }
    }
}
