package net.ximatai.muyun.spring.boot.demo.school.test;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassMember;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClassMemberModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapClassMemberAsUntitledRelationFact() {
        TableWrapper table = mapper.toTable(ClassMember.class);

        assertThat(table.getName()).isEqualTo("education_class_member");
        assertThat(columnNames(table)).contains("id", "tenant_id", "classroom_id", "student_id",
                "sort_order", "deleted", "version").doesNotContain("title");
    }

    private Set<String> columnNames(TableWrapper table) {
        Set<String> names = new LinkedHashSet<>();
        if (table.getPrimaryKey() != null) {
            names.add(table.getPrimaryKey().getName());
        }
        table.getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }
}
