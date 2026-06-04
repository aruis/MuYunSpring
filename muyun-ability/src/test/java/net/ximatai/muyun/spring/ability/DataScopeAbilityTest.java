package net.ximatai.muyun.spring.ability;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.standard.StandardDataScopedEntity;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataScopeAbilityTest {
    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        ActionExecutionContextHolder.clear();
        TenantContext.clear();
    }

    @Test
    void shouldApplyDataScopeOnlyForExplicitReadActions() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new OwnerDataScopeCriteriaService());
        String ownId;
        String othersId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            ownId = service.insert(record("Own", "user-1"));
            othersId = service.insert(record("Others", "user-2"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            assertThat(service.pageQueryForAction(PlatformAction.QUERY, Criteria.of(), PageRequest.of(1, 10))
                    .getRecords())
                    .extracting(DemoDataScopedRecord::getTitle)
                    .containsExactly("Own");
            assertThat(service.countForAction(PlatformAction.QUERY, Criteria.of())).isEqualTo(1);
            assertThat(service.selectForAction(PlatformAction.VIEW, ownId)).isNotNull();
            assertThat(service.afterSelectCount).isEqualTo(1);
            assertThat(service.selectForAction(PlatformAction.VIEW, othersId)).isNull();

            assertThat(service.select(othersId)).isNotNull();
            DemoDataScopedRecord update = record("Others updated", "user-2");
            update.setId(othersId);
            assertThatThrownBy(() -> service.update(update))
                    .hasMessageContaining("record data permission denied");
        }
    }

    @Test
    void shouldAllowDataScopedMutationWhenRecordIsVisibleForAction() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new OwnerDataScopeCriteriaService());
        String ownId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            ownId = service.insert(record("Own", "user-1"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            DemoDataScopedRecord update = record("Own updated", "user-1");
            update.setId(ownId);
            assertThat(service.update(update)).isEqualTo(1);
            assertThat(service.select(ownId).getTitle()).isEqualTo("Own updated");
        }
    }

    @Test
    void shouldKeepTenantScopeWhenDataScopeAllowsAllData() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new AllDataScopeCriteriaService());
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.insert(record("Tenant A", "user-1"));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            service.insert(record("Tenant B", "user-1"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            assertThat(service.pageQueryForAction(PlatformAction.QUERY, Criteria.of(), PageRequest.of(1, 10))
                    .getRecords())
                    .extracting(DemoDataScopedRecord::getTitle)
                    .containsExactly("Tenant A");
        }
    }

    @Test
    void shouldBypassTenantScopeWhenDataScopeAllowsCrossTenantData() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new CrossTenantAllDataScopeCriteriaService());
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.insert(record("Tenant A", "user-1"));
        }
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            service.insert(record("Tenant B", "user-1"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            assertThat(service.pageQueryForAction(PlatformAction.QUERY, Criteria.of(), PageRequest.of(1, 10))
                    .getRecords())
                    .extracting(DemoDataScopedRecord::getTitle)
                    .containsExactly("Tenant A", "Tenant B");
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
            assertThat(TenantContext.tenantFilterBypassed()).isFalse();
        }
    }

    @Test
    void shouldBypassTenantScopeForMutationWhenDataScopeAllowsCrossTenantData() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new CrossTenantAllDataScopeCriteriaService());
        String tenantBId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            tenantBId = service.insert(record("Tenant B", "user-1"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            DemoDataScopedRecord update = record("Tenant B updated", "user-1");
            update.setId(tenantBId);

            assertThat(service.update(update)).isEqualTo(1);
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
            assertThat(TenantContext.tenantFilterBypassed()).isFalse();
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            assertThat(service.select(tenantBId).getTitle()).isEqualTo("Tenant B updated");
        }
    }

    @Test
    void shouldUseCurrentActionPolicyWhenCheckingMutationScope() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new OwnerDataScopeCriteriaService());
        String othersId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            othersId = service.insert(record("Others", "user-2"));
        }

        ActionExecutionPolicy bypassDataScope = new ActionExecutionPolicy(
                "normalize",
                PlatformActionLevel.RECORD,
                ActionAccessMode.AUTH_REQUIRED,
                true,
                false,
                ActionDefaultGrantPolicy.NONE,
                null
        );
        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"));
             ActionExecutionContextHolder.Scope action = ActionExecutionContextHolder.use(ActionExecutionContext.ofPolicy(
                     "demo.dataScoped", bypassDataScope, java.util.Set.of(othersId), CurrentUserContext.currentUser()))) {
            DemoDataScopedRecord update = record("Others normalized", "user-2");
            update.setId(othersId);
            assertThat(service.update(update)).isEqualTo(1);
            assertThat(service.select(othersId).getTitle()).isEqualTo("Others normalized");
        }
    }

    @Test
    void shouldKeepBusinessReadOverridesWhenApplyingDataScope() {
        OverridingDataScopedRecordService service = new OverridingDataScopedRecordService(new OwnerDataScopeCriteriaService());
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.insert(record("Own", "user-1"));
            service.insert(record("Others", "user-2"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            assertThat(service.pageQueryForAction(PlatformAction.QUERY, Criteria.of(), PageRequest.of(1, 10))
                    .getRecords())
                    .extracting(DemoDataScopedRecord::getTitle)
                    .containsExactly("Own");
            assertThat(service.pageQueryOverrideCount).isEqualTo(1);
        }
    }

    @Test
    void shouldNotTrustUnrestrictedFlagWhenActionCriteriaIsChanged() {
        DemoDataScopedRecordService service = new DemoDataScopedRecordService(new MisflaggedOwnerDataScopeCriteriaService());
        String othersId;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.insert(record("Own", "user-1"));
            othersId = service.insert(record("Others", "user-2"));
        }

        try (TenantContext.Scope tenant = TenantContext.use("tenant-a");
             CurrentUserContext.Scope user = CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            assertThat(service.selectForAction(PlatformAction.VIEW, othersId)).isNull();
            assertThat(service.afterSelectCount).isZero();
        }
    }

    private DemoDataScopedRecord record(String title, String authUserId) {
        DemoDataScopedRecord record = new DemoDataScopedRecord();
        record.setTitle(title);
        record.setAuthUserId(authUserId);
        return record;
    }

    @Getter
    @Setter
    private static final class DemoDataScopedRecord extends StandardDataScopedEntity {
        private String title;
    }

    private static final class DemoDataScopedRecordService extends AbstractAbilityService<DemoDataScopedRecord>
            implements DataScopeAbility<DemoDataScopedRecord> {
        private final DataScopeCriteriaService dataScopeCriteriaService;
        private int afterSelectCount;

        private DemoDataScopedRecordService(DataScopeCriteriaService dataScopeCriteriaService) {
            super("demo.dataScoped", DemoDataScopedRecord.class, new InMemoryBaseDao<>());
            this.dataScopeCriteriaService = dataScopeCriteriaService;
        }

        @Override
        public DataScopeCriteriaService getDataScopeCriteriaService() {
            return dataScopeCriteriaService;
        }

        @Override
        public void afterSelect(DemoDataScopedRecord entity) {
            afterSelectCount++;
        }
    }

    private static final class OverridingDataScopedRecordService extends AbstractAbilityService<DemoDataScopedRecord>
            implements DataScopeAbility<DemoDataScopedRecord> {
        private final DataScopeCriteriaService dataScopeCriteriaService;
        private int pageQueryOverrideCount;

        private OverridingDataScopedRecordService(DataScopeCriteriaService dataScopeCriteriaService) {
            super("demo.overridingDataScoped", DemoDataScopedRecord.class, new InMemoryBaseDao<>());
            this.dataScopeCriteriaService = dataScopeCriteriaService;
        }

        @Override
        public DataScopeCriteriaService getDataScopeCriteriaService() {
            return dataScopeCriteriaService;
        }

        @Override
        public net.ximatai.muyun.database.core.orm.PageResult<DemoDataScopedRecord> pageQuery(Criteria criteria,
                                                                                              PageRequest pageRequest,
                                                                                              net.ximatai.muyun.database.core.orm.Sort... sorts) {
            pageQueryOverrideCount++;
            return DataScopeAbility.super.pageQuery(criteria, pageRequest, sorts);
        }
    }

    private static final class OwnerDataScopeCriteriaService implements DataScopeCriteriaService {
        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        String actionCode,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return DataScopeCriteriaResult.restricted(applyReadScope(moduleAlias, actionCode, criteria, currentUser));
        }

        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        ActionExecutionPolicy policy,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return resolveReadScope(moduleAlias, policy.permissionActionCode(), criteria, currentUser);
        }

        @Override
        public Criteria applyReadScope(String moduleAlias,
                                       String actionCode,
                                       Criteria criteria,
                                       java.util.Optional<CurrentUser> currentUser) {
            Criteria scoped = Criteria.of();
            if (criteria != null && !criteria.isEmpty()) {
                scoped.andGroup(criteria.getRoot());
            }
            scoped.eq(PlatformAbilityFields.AUTH_USER_FIELD, currentUser.orElseThrow().userId());
            return scoped;
        }
    }

    private static final class AllDataScopeCriteriaService implements DataScopeCriteriaService {
        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        String actionCode,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return DataScopeCriteriaResult.unrestricted(criteria == null ? Criteria.of() : criteria);
        }

        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        ActionExecutionPolicy policy,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return resolveReadScope(moduleAlias, policy.permissionActionCode(), criteria, currentUser);
        }
    }

    private static final class CrossTenantAllDataScopeCriteriaService implements DataScopeCriteriaService {
        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        String actionCode,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return DataScopeCriteriaResult.crossTenantUnrestricted(criteria == null ? Criteria.of() : criteria);
        }

        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        ActionExecutionPolicy policy,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return resolveReadScope(moduleAlias, policy.permissionActionCode(), criteria, currentUser);
        }
    }

    private static final class MisflaggedOwnerDataScopeCriteriaService implements DataScopeCriteriaService {
        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        String actionCode,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return DataScopeCriteriaResult.unrestricted(applyReadScope(moduleAlias, actionCode, criteria, currentUser));
        }

        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        ActionExecutionPolicy policy,
                                                        Criteria criteria,
                                                        java.util.Optional<CurrentUser> currentUser) {
            return resolveReadScope(moduleAlias, policy.permissionActionCode(), criteria, currentUser);
        }

        @Override
        public Criteria applyReadScope(String moduleAlias,
                                       String actionCode,
                                       Criteria criteria,
                                       java.util.Optional<CurrentUser> currentUser) {
            Criteria scoped = Criteria.of();
            if (criteria != null && !criteria.isEmpty()) {
                scoped.andGroup(criteria.getRoot());
            }
            scoped.eq(PlatformAbilityFields.AUTH_USER_FIELD, currentUser.orElseThrow().userId());
            return scoped;
        }
    }
}
