package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.spring.boot.JdbiConfigurer;
import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.publish.DynamicModulePublisher;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DynamicCodeCoordinatorRepositoryIT.TestApplication.class)
class DynamicCodeCoordinatorRepositoryIT {
    private static final String TENANT_ID = "tenant-code";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.default-schema", () -> "public");
        registry.add("muyun.database.repository-schema-mode", () -> "ENSURE");
    }

    private final DynamicModulePublisher publisher;
    private final DynamicRecordService recordService;
    private final CodeRuleService ruleService;
    private final CodeSequenceStateService stateService;
    private final CodeLedgerEntryService ledgerService;
    private final CodeRecycleEntryService recycleService;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    DynamicCodeCoordinatorRepositoryIT(DynamicModulePublisher publisher,
                                       DynamicRecordService recordService,
                                       CodeRuleService ruleService,
                                       CodeSequenceStateService stateService,
                                       CodeLedgerEntryService ledgerService,
                                       CodeRecycleEntryService recycleService,
                                       PlatformTransactionManager transactionManager) {
        this.publisher = publisher;
        this.recordService = recordService;
        this.ruleService = ruleService;
        this.stateService = stateService;
        this.ledgerService = ledgerService;
        this.recycleService = recycleService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldAssignCodeAndLedgerThroughDynamicCreateOnRealRuntime() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO, true);

        DynamicRecord record = create(scenario, "first");

        assertThat(record.getValue("orderNo")).isEqualTo("SO-0001");
        DynamicRecord selected = recordService.select(scenario.moduleAlias(), "main", record.getId());
        assertThat(selected.getValue("orderNo")).isEqualTo("SO-0001");
        CodeLedgerEntry ledger = ledgerService.findByRuleAndValue(rule.getId(), "SO-0001");
        assertThat(ledger.getStatus()).isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(ledger.getSourceRecordId()).isEqualTo(record.getId());
        assertThat(stateService.selectState(rule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET).getCurrentValue()).isEqualTo(1L);
    }

    @Test
    void shouldRejectManualValueForAutoAndKeepManualValueForEditableAutoRule() {
        Scenario autoScenario = publishScenario();
        CodeRule autoRule = saveRule(autoScenario, CodeMode.AUTO, true);

        assertThatThrownBy(() -> create(autoScenario, "invalid", "MANUAL-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("AUTO code field does not accept manual value");
        assertThat(records(autoScenario)).isEmpty();
        assertThat(stateService.selectState(autoRule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET)).isNull();
        assertThat(ledgerEntries(autoRule)).isEmpty();

        Scenario editableScenario = publishScenario();
        CodeRule editableRule = saveRule(editableScenario, CodeMode.AUTO_WITH_MANUAL_EDIT, true);
        DynamicRecord manual = create(editableScenario, "manual", "KEEP-1");

        assertThat(manual.getValue("orderNo")).isEqualTo("KEEP-1");
        assertThat(stateService.selectState(editableRule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET)).isNull();
        CodeLedgerEntry ledger = ledgerService.findByRuleAndValue(editableRule.getId(), "KEEP-1");
        assertThat(ledger.getStatus()).isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(ledger.getSourceRecordId()).isEqualTo(manual.getId());
    }

    @Test
    void shouldIgnoreDisabledAndUnmatchedRules() {
        Scenario disabledScenario = publishScenario();
        CodeRule disabledRule = saveRule(disabledScenario, CodeMode.AUTO, false);
        Scenario unmatchedScenario = publishScenario();

        DynamicRecord disabled = create(disabledScenario, "disabled");
        DynamicRecord unmatched = create(unmatchedScenario, "unmatched");

        assertThat(disabled.getValue("orderNo")).isNull();
        assertThat(unmatched.getValue("orderNo")).isNull();
        assertThat(stateService.selectState(disabledRule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET)).isNull();
        assertThat(ledgerEntries(disabledRule)).isEmpty();
    }

    @Test
    void shouldGenerateMissingCodeDuringDynamicUpdate() {
        Scenario scenario = publishScenario();
        DynamicRecord record = create(scenario, "draft");
        CodeRule rule = saveRule(scenario, CodeMode.AUTO, true);

        DynamicRecord update = recordService.newRecord(scenario.moduleAlias(), "main")
                .setValue("title", "confirmed");
        update.setId(record.getId());
        recordService.update(scenario.moduleAlias(), "main", update);

        assertThat(update.getValue("orderNo")).isEqualTo("SO-0001");
        DynamicRecord selected = recordService.select(scenario.moduleAlias(), "main", record.getId());
        assertThat(selected.getValue("title")).isEqualTo("confirmed");
        assertThat(selected.getValue("orderNo")).isEqualTo("SO-0001");
        CodeLedgerEntry ledger = ledgerService.findByRuleAndValue(rule.getId(), "SO-0001");
        assertThat(ledger.getStatus()).isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(ledger.getSourceRecordId()).isEqualTo(record.getId());
    }

    @Test
    void shouldRollbackDynamicCreateSequenceStateAndLedgerWithOuterTransaction() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO, true);
        AtomicReference<String> recordId = new AtomicReference<>();

        transactionTemplate.executeWithoutResult(status -> {
            try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
                DynamicRecord record = recordService.newRecord(scenario.moduleAlias(), "main")
                        .setValue("title", "rollback");
                recordService.create(scenario.moduleAlias(), "main", record);
                recordId.set(record.getId());

                assertThat(recordService.select(scenario.moduleAlias(), "main", record.getId())).isNotNull();
                assertThat(stateService.selectState(rule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                        CodeSequenceState.DEFAULT_BUCKET)).isNotNull();
                assertThat(ledgerService.findByRuleAndValue(rule.getId(), "SO-0001")).isNotNull();
                status.setRollbackOnly();
            }
        });

        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            assertThat(recordService.select(scenario.moduleAlias(), "main", recordId.get())).isNull();
            assertThat(stateService.selectState(rule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                    CodeSequenceState.DEFAULT_BUCKET)).isNull();
            assertThat(ledgerEntries(rule)).isEmpty();
        }
    }

    @Test
    void shouldReleaseDeletedDynamicCodeToAvailableOrDiscardedLedgerFact() {
        Scenario reusableScenario = publishScenario();
        CodeRule reusableRule = saveRule(reusableScenario, CodeMode.AUTO, true);
        DynamicRecord reusable = create(reusableScenario, "reusable");

        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            recordService.delete(reusableScenario.moduleAlias(), "main", reusable.getId());
        }

        CodeLedgerEntry reusableLedger = ledgerService.findByRuleAndValue(reusableRule.getId(), "SO-0001");
        assertThat(reusableLedger.getStatus()).isEqualTo(CodeLedgerStatus.AVAILABLE);
        assertThat(reusableLedger.getLastAction()).isEqualTo(CodeLedgerAction.RELEASED_BY_DELETE);
        assertThat(reusableLedger.getSourceRecordId()).isNull();
        CodeRecycleEntry reusableRecycle = recycleEntry(reusableRule, "SO-0001");
        assertThat(reusableRecycle.getStatus()).isEqualTo(CodeRecycleStatus.AVAILABLE);

        Scenario discardScenario = publishScenario();
        CodeRule discardRule = saveRule(discardScenario, CodeMode.AUTO, true, false);
        DynamicRecord discarded = create(discardScenario, "discarded");

        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            recordService.delete(discardScenario.moduleAlias(), "main", discarded.getId());
        }

        CodeLedgerEntry discardLedger = ledgerService.findByRuleAndValue(discardRule.getId(), "SO-0001");
        assertThat(discardLedger.getStatus()).isEqualTo(CodeLedgerStatus.DISCARDED);
        assertThat(discardLedger.getLastAction()).isEqualTo(CodeLedgerAction.RELEASED_BY_DELETE);
        assertThat(recycleEntry(discardRule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.DISCARDED);
    }

    @Test
    void shouldReleaseCodesWhenDynamicRecordsAreDeletedInBatch() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO, true);
        DynamicRecord first = create(scenario, "first");
        DynamicRecord second = create(scenario, "second");

        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            assertThat(recordService.deleteBatch(scenario.moduleAlias(), "main", List.of(first.getId(), second.getId())))
                    .isEqualTo(2);
        }

        CodeLedgerEntry firstLedger = ledgerService.findByRuleAndValue(rule.getId(), "SO-0001");
        assertThat(firstLedger.getStatus()).isEqualTo(CodeLedgerStatus.AVAILABLE);
        assertThat(firstLedger.getLastAction()).isEqualTo(CodeLedgerAction.RELEASED_BY_DELETE);
        assertThat(firstLedger.getSourceRecordId()).isNull();
        assertThat(recycleEntry(rule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.AVAILABLE);

        CodeLedgerEntry secondLedger = ledgerService.findByRuleAndValue(rule.getId(), "SO-0002");
        assertThat(secondLedger.getStatus()).isEqualTo(CodeLedgerStatus.AVAILABLE);
        assertThat(secondLedger.getSourceRecordId()).isNull();
        assertThat(recycleEntry(rule, "SO-0002").getStatus()).isEqualTo(CodeRecycleStatus.AVAILABLE);
    }

    @Test
    void shouldReleaseOldCodeAndBindManualCodeWhenEditableAutoIsManuallyChanged() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO_WITH_MANUAL_EDIT, true);
        DynamicRecord record = create(scenario, "editable");

        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            DynamicRecord update = recordService.newRecord(scenario.moduleAlias(), "main")
                    .setValue("orderNo", "MANUAL-9");
            update.setId(record.getId());
            recordService.update(scenario.moduleAlias(), "main", update);
        }

        assertThat(recordService.select(scenario.moduleAlias(), "main", record.getId()).getValue("orderNo"))
                .isEqualTo("MANUAL-9");
        CodeLedgerEntry oldLedger = ledgerService.findByRuleAndValue(rule.getId(), "SO-0001");
        assertThat(oldLedger.getStatus()).isEqualTo(CodeLedgerStatus.AVAILABLE);
        assertThat(oldLedger.getLastAction()).isEqualTo(CodeLedgerAction.RELEASED_BY_MANUAL_EDIT);
        assertThat(recycleEntry(rule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.AVAILABLE);
        CodeLedgerEntry newLedger = ledgerService.findByRuleAndValue(rule.getId(), "MANUAL-9");
        assertThat(newLedger.getStatus()).isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(newLedger.getSourceRecordId()).isEqualTo(record.getId());
    }

    @Test
    void shouldReleaseOldCodeAndBindRegeneratedCodeWhenLinkedFieldChanges() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveLinkedRule(scenario, true);
        DynamicRecord record = create(scenario, "alpha");

        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            DynamicRecord update = recordService.newRecord(scenario.moduleAlias(), "main")
                    .setValue("title", "beta");
            update.setId(record.getId());
            recordService.update(scenario.moduleAlias(), "main", update);
        }

        DynamicRecord selected = recordService.select(scenario.moduleAlias(), "main", record.getId());
        assertThat(selected.getValue("orderNo")).isEqualTo("SO-beta-0001");
        CodeLedgerEntry oldLedger = ledgerService.findByRuleAndValue(rule.getId(), "SO-alpha-0001");
        assertThat(oldLedger.getStatus()).isEqualTo(CodeLedgerStatus.AVAILABLE);
        assertThat(oldLedger.getLastAction()).isEqualTo(CodeLedgerAction.RELEASED_BY_LINKED_UPDATE);
        assertThat(recycleEntry(rule, "SO-alpha-0001").getStatus()).isEqualTo(CodeRecycleStatus.AVAILABLE);
        CodeLedgerEntry newLedger = ledgerService.findByRuleAndValue(rule.getId(), "SO-beta-0001");
        assertThat(newLedger.getStatus()).isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(newLedger.getSourceRecordId()).isEqualTo(record.getId());
    }

    @Test
    void shouldReuseAvailableRecycleOnceAndNeverConsumeDiscardedCode() {
        Scenario reusableScenario = publishScenario();
        CodeRule reusableRule = saveRule(reusableScenario, CodeMode.AUTO, true);
        DynamicRecord first = create(reusableScenario, "first");
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            recordService.delete(reusableScenario.moduleAlias(), "main", first.getId());
        }

        DynamicRecord reused = create(reusableScenario, "reused");
        DynamicRecord next = create(reusableScenario, "next");

        assertThat(reused.getValue("orderNo")).isEqualTo("SO-0001");
        assertThat(next.getValue("orderNo")).isEqualTo("SO-0002");
        assertThat(recycleEntry(reusableRule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.USED);
        assertThat(ledgerService.findByRuleAndValue(reusableRule.getId(), "SO-0001").getStatus())
                .isEqualTo(CodeLedgerStatus.ACTIVE);

        Scenario discardedScenario = publishScenario();
        CodeRule discardedRule = saveRule(discardedScenario, CodeMode.AUTO, true, false);
        DynamicRecord discarded = create(discardedScenario, "discarded");
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            recordService.delete(discardedScenario.moduleAlias(), "main", discarded.getId());
        }
        DynamicRecord afterDiscard = create(discardedScenario, "after-discard");

        assertThat(recycleEntry(discardedRule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.DISCARDED);
        assertThat(afterDiscard.getValue("orderNo")).isEqualTo("SO-0002");
    }

    @Test
    void shouldRejectDuplicateManualBindingAsSingleCurrentLedgerFact() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO_WITH_MANUAL_EDIT, true);
        DynamicRecord first = create(scenario, "first", "DUP-1");

        assertThatThrownBy(() -> create(scenario, "second", "DUP-1"))
                .isInstanceOf(RuntimeException.class);
        assertThat(ledgerService.findByRuleAndValue(rule.getId(), "DUP-1").getSourceRecordId())
                .isEqualTo(first.getId());
        assertThat(records(scenario)).hasSize(1);
    }

    @Test
    void shouldRollbackRecycleConsumptionWhenDynamicCreateFailsInOuterTransaction() {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO, true);
        DynamicRecord first = create(scenario, "first");
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            recordService.delete(scenario.moduleAlias(), "main", first.getId());
        }

        transactionTemplate.executeWithoutResult(status -> {
            try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
                DynamicRecord record = recordService.newRecord(scenario.moduleAlias(), "main")
                        .setValue("title", "rollback-reuse");
                recordService.create(scenario.moduleAlias(), "main", record);
                assertThat(record.getValue("orderNo")).isEqualTo("SO-0001");
                assertThat(recycleEntry(rule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.USED);
                status.setRollbackOnly();
            }
        });

        assertThat(recycleEntry(rule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.AVAILABLE);
        CodeLedgerEntry ledger = ledgerService.findByRuleAndValue(rule.getId(), "SO-0001");
        assertThat(ledger.getStatus()).isEqualTo(CodeLedgerStatus.AVAILABLE);
        assertThat(ledger.getSourceRecordId()).isNull();
    }

    @Test
    void shouldConsumeSingleRecycleCodeOnlyOnceUnderConcurrentDynamicCreates() throws Exception {
        Scenario scenario = publishScenario();
        CodeRule rule = saveRule(scenario, CodeMode.AUTO, true);
        DynamicRecord first = create(scenario, "first");
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            recordService.delete(scenario.moduleAlias(), "main", first.getId());
        }

        int count = 8;
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(count);
        try {
            List<Callable<String>> tasks = IntStream.range(0, count)
                    .mapToObj(i -> (Callable<String>) () -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
                            DynamicRecord record = recordService.newRecord(scenario.moduleAlias(), "main")
                                    .setValue("title", "concurrent-" + i);
                            recordService.create(scenario.moduleAlias(), "main", record);
                            return String.valueOf(record.getValue("orderNo"));
                        }
                    })
                    .toList();
            var futures = tasks.stream().map(executor::submit).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<String> values = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    })
                    .sorted()
                    .toList();

            assertThat(values).hasSize(count).doesNotHaveDuplicates();
            assertThat(values).contains("SO-0001");
            assertThat(values).containsExactlyElementsOf(IntStream.rangeClosed(1, count)
                    .mapToObj(i -> "SO-%04d".formatted(i))
                    .toList());
            assertThat(recycleEntry(rule, "SO-0001").getStatus()).isEqualTo(CodeRecycleStatus.USED);
        } finally {
            executor.shutdownNow();
        }
    }

    private DynamicRecord create(Scenario scenario, String title) {
        return create(scenario, title, null);
    }

    private DynamicRecord create(Scenario scenario, String title, String orderNo) {
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            DynamicRecord record = recordService.newRecord(scenario.moduleAlias(), "main")
                    .setValue("title", title);
            if (orderNo != null) {
                record.setValue("orderNo", orderNo);
            }
            recordService.create(scenario.moduleAlias(), "main", record);
            return record;
        }
    }

    private List<DynamicRecord> records(Scenario scenario) {
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            return recordService.entity(scenario.moduleAlias(), "main")
                    .list(Criteria.of(), PageRequest.of(1, 20));
        }
    }

    private List<CodeLedgerEntry> ledgerEntries(CodeRule rule) {
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            return ledgerService.list(Criteria.of().eq("ruleId", rule.getId()), PageRequest.of(1, 20));
        }
    }

    private CodeRecycleEntry recycleEntry(CodeRule rule, String value) {
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            return recycleService.list(Criteria.of()
                    .eq("ruleId", rule.getId())
                    .eq("recycledValue", value), PageRequest.of(1, 1))
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
    }

    private Scenario publishScenario() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String moduleAlias = "crm.code_it_" + suffix;
        String tableName = "crm_code_it_" + suffix;
        publisher.publish(new ModuleDefinition(moduleAlias, "Code IT", List.of(new EntityDefinition(
                "main",
                tableName,
                "Code IT",
                List.of(
                        FieldDefinition.titleField().required(),
                        FieldDefinition.string("orderNo", "Order No").column("order_no").length(64)
                ),
                Set.of(EntityCapability.CRUD, EntityCapability.REFERENCE)
        ))));
        return new Scenario(moduleAlias);
    }

    private CodeRule saveRule(Scenario scenario, CodeMode mode, boolean enabled) {
        return saveRule(scenario, mode, enabled, true);
    }

    private CodeRule saveRule(Scenario scenario, CodeMode mode, boolean enabled, boolean allowRecycle) {
        CodeRule rule = new CodeRule();
        rule.setModuleAlias(scenario.moduleAlias());
        rule.setEntityAlias("main");
        rule.setFieldName("orderNo");
        rule.setTitle("orderNo");
        rule.setFieldRole(CodeFieldRole.PRIMARY);
        rule.setMode(mode);
        rule.setEnabled(enabled);
        rule.setAllowRecycle(allowRecycle);
        rule.setSegments(List.of(
                segment(CodeSegmentType.CONSTANT, "SO-", null),
                sequenceSegment()
        ));
        CodeSequencePolicy policy = new CodeSequencePolicy();
        policy.setStartValue(1L);
        policy.setStepValue(1L);
        policy.setSequenceLength(4);
        policy.setResetPolicy(CodeSequenceResetPolicy.NONE);
        rule.setSequencePolicy(policy);
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            return ruleService.saveRuleTree(rule);
        }
    }

    private CodeRule saveLinkedRule(Scenario scenario, boolean allowRecycle) {
        CodeRule rule = new CodeRule();
        rule.setModuleAlias(scenario.moduleAlias());
        rule.setEntityAlias("main");
        rule.setFieldName("orderNo");
        rule.setTitle("orderNo");
        rule.setFieldRole(CodeFieldRole.PRIMARY);
        rule.setMode(CodeMode.AUTO);
        rule.setEnabled(Boolean.TRUE);
        rule.setAllowRecycle(allowRecycle);
        rule.setLinkedUpdate(Boolean.TRUE);
        CodeRuleSegment titleBasis = segment(CodeSegmentType.FIELD_VALUE, null, "title");
        titleBasis.setSequenceBasis(Boolean.TRUE);
        titleBasis.setSeparator("-");
        rule.setSegments(List.of(
                segment(CodeSegmentType.CONSTANT, "SO-", null),
                titleBasis,
                sequenceSegment()
        ));
        CodeSequencePolicy policy = new CodeSequencePolicy();
        policy.setStartValue(1L);
        policy.setStepValue(1L);
        policy.setSequenceLength(4);
        policy.setResetPolicy(CodeSequenceResetPolicy.NONE);
        rule.setSequencePolicy(policy);
        try (TenantContext.Scope ignored = TenantContext.use(TENANT_ID)) {
            return ruleService.saveRuleTree(rule);
        }
    }

    private CodeRuleSegment sequenceSegment() {
        CodeRuleSegment segment = segment(CodeSegmentType.SEQUENCE, null, null);
        segment.setLength(4);
        return segment;
    }

    private CodeRuleSegment segment(CodeSegmentType type, String fixedValue, String sourceRef) {
        CodeRuleSegment segment = new CodeRuleSegment();
        segment.setSegmentType(type);
        segment.setFixedValue(fixedValue);
        segment.setSourceRef(sourceRef);
        return segment;
    }

    private record Scenario(String moduleAlias) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EnableMuYunRepositories(basePackageClasses = CodeRuleDao.class)
    static class TestApplication {
        @Bean
        DataSource dataSource() {
            return DataSourceBuilder.create()
                    .url(postgres.getJdbcUrl())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .driverClassName(postgres.getDriverClassName())
                    .build();
        }

        @Bean
        Clock codeClock() {
            return Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        DynamicSchemaService dynamicSchemaService(IDatabaseOperations<?> operations) {
            return new DynamicSchemaService(operations);
        }

        @Bean
        DynamicRecordRuntime dynamicRecordRuntime(IDatabaseOperations<?> operations) {
            return new DynamicRecordRuntime(operations, new DynamicModuleRegistry());
        }

        @Bean
        DynamicModulePublisher dynamicModulePublisher(DynamicSchemaService schemaService,
                                                      DynamicRecordRuntime runtime) {
            return new DynamicModulePublisher(schemaService, runtime);
        }

        @Bean
        DynamicRecordService dynamicRecordService(DynamicRecordRuntime runtime,
                                                  CodeRuleService ruleService,
                                                  CodeGenerateService generateService,
                                                  CodePreviewService previewService,
                                                  CodeLedgerEntryService ledgerEntryService,
                                                  CodeRecycleEntryService recycleEntryService,
                                                  Clock codeClock) {
            DynamicRecordService[] holder = new DynamicRecordService[1];
            DynamicCodeCoordinator coordinator = new DynamicCodeCoordinator(
                    ruleService,
                    generateService,
                    previewService,
                    ledgerEntryService,
                    recycleEntryService,
                    new DynamicRecordServiceProxy(holder),
                    codeClock
            );
            holder[0] = new DynamicRecordService(
                    runtime,
                    new AllowAllActionExecutionPolicyService(),
                    new AllowAllDataScopeCriteriaService(),
                    coordinator
            );
            return holder[0];
        }

        @Bean
        CodeRuleService codeRuleService(CodeRuleDao ruleDao,
                                        CodeRuleSegmentService segmentService,
                                        CodeSequencePolicyService sequencePolicyService,
                                        CodeValueMappingService mappingService) {
            return new CodeRuleService(ruleDao, segmentService, sequencePolicyService, mappingService);
        }

        @Bean
        CodeRuleSegmentService codeRuleSegmentService(CodeRuleSegmentDao segmentDao) {
            return new CodeRuleSegmentService(segmentDao);
        }

        @Bean
        CodeSequencePolicyService codeSequencePolicyService(CodeSequencePolicyDao policyDao) {
            return new CodeSequencePolicyService(policyDao);
        }

        @Bean
        CodeValueMappingService codeValueMappingService(CodeValueMappingDao mappingDao) {
            return new CodeValueMappingService(mappingDao);
        }

        @Bean
        CodeSequenceAllocator codeSequenceAllocator(Jdbi jdbi) {
            return new PostgresCodeSequenceAllocator(jdbi);
        }

        @Bean
        CodeRecycleConsumer codeRecycleConsumer(Jdbi jdbi) {
            return new PostgresCodeRecycleConsumer(jdbi);
        }

        @Bean
        CodeSequenceStateService codeSequenceStateService(CodeSequenceStateDao stateDao,
                                                          CodeSequenceAllocator sequenceAllocator) {
            return new CodeSequenceStateService(stateDao, List.of(sequenceAllocator));
        }

        @Bean
        CodeLedgerEntryService codeLedgerEntryService(CodeLedgerEntryDao ledgerEntryDao) {
            return new CodeLedgerEntryService(ledgerEntryDao);
        }

        @Bean
        CodeRecycleEntryService codeRecycleEntryService(CodeRecycleEntryDao recycleEntryDao,
                                                        CodeRecycleConsumer recycleConsumer) {
            return new CodeRecycleEntryService(recycleEntryDao, List.of(recycleConsumer));
        }

        @Bean
        CodePreviewService codePreviewService(Clock codeClock) {
            return new CodePreviewService(new FormulaEngine(codeClock), codeClock);
        }

        @Bean
        CodeGenerateService codeGenerateService(CodeRuleService ruleService,
                                                CodePreviewService previewService,
                                                CodeSequenceStateService stateService,
                                                CodeRecycleEntryService recycleEntryService,
                                                Clock codeClock) {
            return new CodeGenerateService(ruleService, previewService, stateService, recycleEntryService, codeClock);
        }

        @Bean
        JdbiConfigurer bigIntegerJdbiConfigurer() {
            return jdbi -> jdbi.registerArgument(new AbstractArgumentFactory<BigInteger>(Types.BIGINT) {
                @Override
                protected Argument build(BigInteger value, ConfigRegistry config) {
                    return (position, statement, context) -> statement.setLong(position, value.longValueExact());
                }
            });
        }
    }

    private static final class DynamicRecordServiceProxy extends DynamicRecordService {
        private final DynamicRecordService[] delegate;

        private DynamicRecordServiceProxy(DynamicRecordService[] delegate) {
            super(new DynamicRecordRuntime(mock(IDatabaseOperations.class)));
            this.delegate = delegate;
        }

        @Override
        public net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations entity(String moduleAlias,
                                                                                       String entityAlias) {
            return delegate[0].entity(moduleAlias, entityAlias);
        }
    }
}
