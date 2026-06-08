package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.jdbi.v3.core.Jdbi;

import java.util.Objects;

/**
 * PostgreSQL-only recycle consumer. Register this implementation only for PostgreSQL deployments.
 */
public class PostgresCodeRecycleConsumer implements CodeRecycleConsumer {
    private static final String SQL = """
            update platform_code_recycle_entry
            set
                status = 'USED',
                version = version + 1,
                updated_at = now()
            where id = (
                select id
                from platform_code_recycle_entry
                where (
                        (cast(:tenantId as varchar) is null and tenant_id is null)
                        or tenant_id = :tenantId
                    )
                    and rule_id = :ruleId
                    and basis_key = :basisKey
                    and period_key = :periodKey
                    and status = 'AVAILABLE'
                    and deleted = false
                order by created_at, id
                for update skip locked
                limit 1
            )
            returning id, rule_id, basis_key, period_key, recycled_value, source_record_id, status
            """;

    private final Jdbi jdbi;
    private volatile boolean databaseVerified;

    public PostgresCodeRecycleConsumer(Jdbi jdbi) {
        this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
    }

    @Override
    public CodeRecycleEntry consumeAvailable(String ruleId, String basisKey, String periodKey, String tenantId) {
        verifyPostgres();
        return jdbi.withHandle(handle -> handle.createQuery(SQL)
                .bind("tenantId", effectiveTenantId(tenantId))
                .bind("ruleId", ruleId)
                .bind("basisKey", basisKey)
                .bind("periodKey", periodKey)
                .map((rs, ctx) -> {
                    CodeRecycleEntry entry = new CodeRecycleEntry();
                    entry.setId(rs.getString("id"));
                    entry.setRuleId(rs.getString("rule_id"));
                    entry.setBasisKey(rs.getString("basis_key"));
                    entry.setPeriodKey(rs.getString("period_key"));
                    entry.setRecycledValue(rs.getString("recycled_value"));
                    entry.setSourceRecordId(rs.getString("source_record_id"));
                    entry.setStatus(CodeRecycleStatus.valueOf(rs.getString("status")));
                    return entry;
                })
                .findOne()
                .orElse(null));
    }

    private void verifyPostgres() {
        if (databaseVerified) {
            return;
        }
        synchronized (this) {
            if (databaseVerified) {
                return;
            }
            String productName = jdbi.withHandle(handle -> {
                try {
                    return handle.getConnection().getMetaData().getDatabaseProductName();
                } catch (Exception e) {
                    throw new PlatformException("Unable to detect database product for code recycle consumption", e);
                }
            });
            if (productName == null || !productName.toLowerCase().contains("postgresql")) {
                throw new PlatformException("PostgresCodeRecycleConsumer requires PostgreSQL, current database: " + productName);
            }
            databaseVerified = true;
        }
    }

    private String effectiveTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? null : tenantId;
    }
}
