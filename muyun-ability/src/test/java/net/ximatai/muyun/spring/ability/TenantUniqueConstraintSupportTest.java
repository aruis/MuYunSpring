package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantUniqueConstraintSupportTest {
    @Test
    void insertShouldTranslateDatabaseUniqueViolationToPlatformConflict() {
        RuntimeException persistenceFailure = new RuntimeException(new SQLException("duplicate code", "23505"));
        BaseDao<UniqueRecord, String> dao = new FailingUniqueRecordDao(persistenceFailure, null);

        assertUniqueConflict(() -> new UniqueRecordService(dao).insert(new UniqueRecord("hobby-code")), persistenceFailure);
    }

    @Test
    void updateShouldTranslateDatabaseUniqueViolationToPlatformConflict() {
        RuntimeException persistenceFailure = new RuntimeException(new SQLException("duplicate code", "23505"));
        BaseDao<UniqueRecord, String> dao = new FailingUniqueRecordDao(null, persistenceFailure);
        UniqueRecord record = new UniqueRecord("hobby-code");
        record.setId("record-1");
        record.setVersion(0);

        assertUniqueConflict(() -> new UniqueRecordService(dao).update(record), persistenceFailure);
    }

    @Test
    void shouldPreserveNonUniqueDatabaseFailure() {
        UniqueRecordService service = new UniqueRecordService(new InMemoryBaseDao<>());
        RuntimeException failure = new RuntimeException(new SQLException("foreign key failure", "23503"));

        assertThat(TenantUniqueConstraintSupport.translatePersistFailure(service, new UniqueRecord("hobby-code"), failure))
                .isSameAs(failure);
    }

    @Test
    void shouldPreserveUnmatchedDatabaseUniqueViolation() {
        UniqueRecordService service = new UniqueRecordService(new InMemoryBaseDao<>());
        RuntimeException failure = new RuntimeException(new SQLException("primary key collision", "23505"));

        assertThat(TenantUniqueConstraintSupport.translatePersistFailure(service, new UniqueRecord("hobby-code"), failure))
                .isSameAs(failure);
    }

    @Test
    void preflightDuplicateShouldUseTheSamePlatformConflictContract() {
        UniqueRecordService service = new UniqueRecordService(new InMemoryBaseDao<>());
        service.insert(new UniqueRecord("hobby-code"));

        assertUniqueConflict(() -> service.insert(new UniqueRecord("hobby-code")), null);
    }

    @Test
    void preflightDuplicateShouldExposeSoftDeletedRecordRecovery() {
        UniqueRecordService service = new UniqueRecordService(new InMemoryBaseDao<>());
        UniqueRecord retained = new UniqueRecord("hobby-code");
        service.insert(retained);
        retained.setDeleted(true);
        retained.setDeletedAt(Instant.parse("2026-01-01T00:00:00Z"));

        assertThatThrownBy(() -> service.insert(new UniqueRecord("hobby-code")))
                .isInstanceOfSatisfying(PlatformException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(PlatformErrorCodes.RESOURCE_SOFT_DELETED_CONFLICT);
                    assertThat(exception.httpStatus()).isEqualTo(409);
                    assertThat(exception.details()).containsEntry("resourceModuleAlias", "demo.uniqueRecord")
                            .containsEntry("resourceRecordId", retained.getId())
                            .containsEntry("recoveryAvailable", Boolean.TRUE);
                });
    }

    private void assertUniqueConflict(ThrowingCallable operation, RuntimeException persistenceFailure) {
        assertThatThrownBy(operation::call)
                .isInstanceOfSatisfying(PlatformException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(PlatformErrorCodes.CONFLICT_UNIQUE);
                    assertThat(exception.httpStatus()).isEqualTo(409);
                    assertThat(exception).hasMessage("hobby code already exists");
                    assertThat(exception.getCause()).isSameAs(persistenceFailure);
                    assertThat(exception.scope().moduleAlias()).isEqualTo("demo.uniqueRecord");
                    assertThat(exception.details()).containsEntry("fields", List.of("code"));
                });
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }

    @TenantUniqueConstraint(fields = "code", message = "hobby code already exists")
    private static final class UniqueRecord extends StandardEntity {
        private final String code;

        private UniqueRecord(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private static final class UniqueRecordService extends AbstractAbilityService<UniqueRecord> {
        private UniqueRecordService(BaseDao<UniqueRecord, String> dao) {
            super("demo.uniqueRecord", UniqueRecord.class, dao);
        }
    }

    private static final class FailingUniqueRecordDao extends InMemoryBaseDao<UniqueRecord> {
        private final RuntimeException insertFailure;
        private final RuntimeException updateFailure;
        private int queryCount;

        private FailingUniqueRecordDao(RuntimeException insertFailure, RuntimeException updateFailure) {
            this.insertFailure = insertFailure;
            this.updateFailure = updateFailure;
        }

        @Override
        public String insert(UniqueRecord entity) {
            if (insertFailure != null) {
                throw insertFailure;
            }
            return super.insert(entity);
        }

        @Override
        public int updateByIdAndVersion(UniqueRecord entity, Integer expectedVersion) {
            if (updateFailure != null) {
                throw updateFailure;
            }
            return super.updateByIdAndVersion(entity, expectedVersion);
        }

        @Override
        public List<UniqueRecord> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            if (queryCount++ > 0) {
                UniqueRecord conflict = new UniqueRecord("hobby-code");
                conflict.setId("concurrent-record");
                return List.of(conflict);
            }
            return super.query(criteria, pageRequest, sorts);
        }
    }
}
