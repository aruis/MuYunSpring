package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.database.core.orm.Sort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuerySchemaTest {
    @Test
    void shouldExposeDescriptorAsFrontendConsumableSchema() {
        QueryDescriptor descriptor = QueryDescriptor.builder("iam.employee")
                .field(QueryField.of("employeeNo", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("职员编号")
                        .withQuickSearch()
                        .withSortable())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ)
                        .withTitle("启用状态"))
                .externalCriteria("departmentScope", value -> null)
                .defaultSort(Sort.asc("employeeNo"))
                .build();

        QuerySchema schema = QuerySchema.from(descriptor);

        assertThat(schema.scopeName()).isEqualTo("iam.employee");
        assertThat(schema.entityAlias()).isNull();
        assertThat(schema.quickSearch().enabled()).isTrue();
        assertThat(schema.quickSearch().fields()).containsExactly("employeeNo");
        assertThat(schema.quickSearch().fieldSchemas()).singleElement()
                .extracting(QuerySchema.Field::title)
                .isEqualTo("职员编号");
        assertThat(schema.fields()).hasSize(2);
        assertThat(schema.fields().getFirst().name()).isEqualTo("employeeNo");
        assertThat(schema.fields().getFirst().title()).isEqualTo("职员编号");
        assertThat(schema.fields().getFirst().operators()).containsExactly(QueryOperator.EQ, QueryOperator.LIKE);
        assertThat(schema.fields().getFirst().defaultOperator()).isEqualTo(QueryOperator.LIKE);
        assertThat(schema.fields().getFirst().quickSearch()).isTrue();
        assertThat(schema.fields().getFirst().sortable()).isTrue();
        assertThat(schema.externalCriteria()).singleElement().satisfies(criteria -> {
            assertThat(criteria.key()).isEqualTo("departmentScope");
            assertThat(criteria.valueType()).isEqualTo("OBJECT");
            assertThat(criteria.providedBy()).isEqualTo("PAGE_CONTEXT");
        });
        assertThat(schema.defaultSorts()).singleElement().satisfies(sort -> {
            assertThat(sort.field()).isEqualTo("employeeNo");
            assertThat(sort.desc()).isFalse();
        });
    }
}
