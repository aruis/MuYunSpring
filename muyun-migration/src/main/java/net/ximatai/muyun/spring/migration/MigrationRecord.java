package net.ximatai.muyun.spring.migration;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

/**
 * Records the highest migration version applied for a given migration alias.
 *
 * <p>The {@code alias} column is declared unique; the platform normalizes that into a
 * {@code (tenant_id, alias)} composite unique index. Today this table is global and
 * {@code tenant_id} is always null, so {@code alias} is effectively globally unique. When
 * per-tenant migration is introduced later, the same table and index serve as-is — only the
 * {@link MigrationVersionStore} implementation changes.
 *
 * <p>The applied version field is named {@code applied_version} (not {@code version}) to avoid
 * colliding with the platform's optimistic-lock {@code version} column inherited from
 * {@link StandardEntity}.
 */
@Getter
@Setter
@Table(name = "migration_record", comment = "Data migration version tracker")
public class MigrationRecord extends StandardEntity {

    @Column(name = "alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            unique = true, comment = "Migration alias, globally unique today")
    private String alias;

    @Column(name = "applied_version", type = ColumnType.INT, nullable = false,
            defaultVal = @Default(number = 0), comment = "Highest applied migration version")
    private Integer appliedVersion = 0;
}
