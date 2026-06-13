package net.ximatai.muyun.spring.migration;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
@Table(name = "test_person", comment = "Test person for migration")
public class TestPerson extends StandardEntity {

    @Column(name = "name", type = ColumnType.VARCHAR, length = 64)
    private String name;

    @Column(name = "age", type = ColumnType.INT)
    private Integer age;
}
