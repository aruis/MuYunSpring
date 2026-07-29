package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.platform.currency.Currency;
import net.ximatai.muyun.spring.platform.currency.ExchangeRate;
import net.ximatai.muyun.spring.platform.currency.ExchangeRateType;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.measure.MeasureUnit;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategory;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TenantUniqueConstraintSchemaContractTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @ParameterizedTest
    @MethodSource("tenantUniqueModels")
    void shouldPreservePromotedTenantUniqueIndexColumns(Class<?> modelClass, List<List<String>> expectedIndexes) {
        assertThat(tenantUniqueIndexes(mapper.toTable(modelClass)))
                .containsExactlyInAnyOrderElementsOf(expectedIndexes);
    }

    private static Stream<Arguments> tenantUniqueModels() {
        return Stream.of(
                Arguments.of(Currency.class, List.of(List.of("tenant_id", "code"))),
                Arguments.of(ExchangeRateType.class, List.of(List.of("tenant_id", "code"))),
                Arguments.of(ExchangeRate.class, List.of(List.of("tenant_id", "from_currency_code", "to_currency_code",
                        "rate_type_code", "effective_date"))),
                Arguments.of(MeasureUnitCategory.class, List.of(List.of("tenant_id", "application_alias", "alias"))),
                Arguments.of(MeasureUnit.class, List.of(List.of("tenant_id", "application_alias", "category_alias", "code"))),
                Arguments.of(DictionaryCategory.class, List.of(List.of("tenant_id", "application_alias", "alias"))),
                Arguments.of(DictionaryItem.class, List.of(
                        List.of("tenant_id", "category_id", "code"),
                        List.of("tenant_id", "category_id", "title")
                )),
                Arguments.of(WorkflowDefinition.class, List.of(List.of("tenant_id", "module_alias", "alias"))),
                Arguments.of(WorkflowTaskDefinition.class, List.of(List.of("tenant_id", "module_alias", "alias")))
        );
    }

    private List<List<String>> tenantUniqueIndexes(TableWrapper table) {
        return table.getIndexes().stream()
                .filter(index -> index.isUnique() && !index.getColumns().isEmpty()
                        && "tenant_id".equals(index.getColumns().getFirst()))
                .map(index -> List.copyOf(index.getColumns()))
                .toList();
    }
}
