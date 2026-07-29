package net.ximatai.muyun.spring.iam;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionCategory;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.user.UserAccount;
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
                Arguments.of(Organization.class, List.of(List.of("tenant_id", "code"))),
                Arguments.of(Department.class, List.of(List.of("tenant_id", "organization_id", "code"))),
                Arguments.of(Employee.class, List.of(List.of("tenant_id", "organization_id", "employee_no"))),
                Arguments.of(UserAccount.class, List.of(List.of("tenant_id", "username"))),
                Arguments.of(Position.class, List.of(List.of("tenant_id", "code"))),
                Arguments.of(PositionCategory.class, List.of(List.of("tenant_id", "code"))),
                Arguments.of(Role.class, List.of(List.of("tenant_id", "owner_scope_type", "owner_scope_key",
                        "assignment_type", "role_kind", "title")))
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
