package net.ximatai.muyun.spring.boot.platform;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.code.CodeFieldRole;
import net.ximatai.muyun.spring.platform.code.CodeGenerateService;
import net.ximatai.muyun.spring.platform.code.CodeIssueLogService;
import net.ximatai.muyun.spring.platform.code.CodeIssueLogStatus;
import net.ximatai.muyun.spring.platform.code.CodeLedgerAction;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntryService;
import net.ximatai.muyun.spring.platform.code.CodeLedgerStatus;
import net.ximatai.muyun.spring.platform.code.CodeMode;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntryService;
import net.ximatai.muyun.spring.platform.code.CodeRecycleStatus;
import net.ximatai.muyun.spring.platform.code.CodeRule;
import net.ximatai.muyun.spring.platform.code.CodeRuleSegment;
import net.ximatai.muyun.spring.platform.code.CodeRuleService;
import net.ximatai.muyun.spring.platform.code.CodeSegmentType;
import net.ximatai.muyun.spring.platform.code.CodeSequencePolicy;
import net.ximatai.muyun.spring.platform.code.CodeSequenceResetPolicy;
import net.ximatai.muyun.spring.platform.code.CodeSequenceState;
import net.ximatai.muyun.spring.platform.code.CodeSequenceStateService;
import net.ximatai.muyun.spring.platform.code.GenerateCodeCommand;
import net.ximatai.muyun.spring.platform.code.GenerateCodeResult;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@TestProfile(CodeRepositoryIT.PostgresProfile.class)
@QuarkusTestResource(value = PostgresQuarkusTestResource.class, restrictToAnnotatedClass = true)
class CodeRepositoryIT {

    @Inject
    Config config;

    @Inject
    CodeRuleService ruleService;

    @Inject
    CodeGenerateService generateService;

    @Inject
    CodeSequenceStateService stateService;

    @Inject
    CodeLedgerEntryService ledgerService;

    @Inject
    CodeRecycleEntryService recycleService;

    @Inject
    CodeIssueLogService issueLogService;

    @Inject
    UserTransaction userTransaction;

    @Test
    void shouldPersistCodeRuleTreeAndLifecycleRecordsThroughRepository() {
        requirePostgres();
        CodeRule rule = rule(uniqueModuleAlias(), "orderNo");
        rule.setAllowRecycle(Boolean.TRUE);
        CodeRule saved = ruleService.saveRuleTree(rule);

        CodeRule viewed = ruleService.viewRuleTree(saved.getId());
        assertThat(viewed.getSegments()).hasSize(2);
        assertThat(viewed.getSequencePolicy().getSequenceLength()).isEqualTo(4);

        GenerateCodeResult generated = generateService.generate(new GenerateCodeCommand(
                rule.getModuleAlias(),
                "main",
                null,
                "orderNo",
                null,
                LocalDateTime.parse("2026-06-08T10:00:00"),
                Map.of(),
                null
        ));
        assertThat(issueLogService.selectByRuleId(saved.getId(), 10)).singleElement()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(CodeIssueLogStatus.SUCCESS);
                    assertThat(log.getGeneratedValue()).isEqualTo("SO-0001");
                });
        ledgerService.upsertActiveBinding(rule, generated.value(), generated.basisKey(), generated.periodKey(), "order-1");
        recycleService.record(rule, generated.basisKey(), generated.periodKey(), generated.value(), "order-1");

        CodeSequenceState state = stateService.selectState(rule.getId(), generated.basisKey(), generated.periodKey());
        assertThat(state.getCurrentValue()).isEqualTo(1L);
        assertThat(ledgerService.findByRuleAndValue(rule.getId(), generated.value()).getStatus())
                .isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(recycleService.list(Criteria.of().eq("ruleId", rule.getId()), PageRequest.of(1, 10)))
                .extracting(CodeRecycleEntry::getRecycledValue)
                .containsExactly(generated.value());
    }

    @Test
    void shouldRollbackSequenceAllocationWithOuterTransaction() throws Exception {
        requirePostgres();
        CodeRule rule = rule(uniqueModuleAlias(), "orderNo");
        ruleService.saveRuleTree(rule);

        userTransaction.begin();
        try {
            GenerateCodeResult generated = generateService.generate(new GenerateCodeCommand(
                    rule.getModuleAlias(),
                    "main",
                    null,
                    "orderNo",
                    null,
                    LocalDateTime.parse("2026-06-08T10:00:00"),
                    Map.of(),
                    null
            ));
            assertThat(generated.value()).isEqualTo("SO-0001");
            assertThat(stateService.selectState(rule.getId(), generated.basisKey(), generated.periodKey()))
                    .isNotNull();
        } finally {
            userTransaction.rollback();
        }

        assertThat(stateService.selectState(rule.getId(), CodeSequenceState.DEFAULT_BUCKET, CodeSequenceState.DEFAULT_BUCKET))
                .isNull();
        assertThat(generateService.generate(new GenerateCodeCommand(
                rule.getModuleAlias(),
                "main",
                null,
                "orderNo",
                null,
                LocalDateTime.parse("2026-06-08T10:00:00"),
                Map.of(),
                null
        )).value()).isEqualTo("SO-0001");
    }

    @Test
    void shouldAllocateSequenceAtomicallyUnderConcurrentRepositoryWrites() throws Exception {
        requirePostgres();
        CodeRule rule = rule(uniqueModuleAlias(), "orderNo");
        ruleService.saveRuleTree(rule);
        int count = 24;
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(count);
        try {
            List<Callable<String>> tasks = IntStream.range(0, count)
                    .mapToObj(i -> (Callable<String>) () -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        try (TenantContext.Scope ignored = TenantContext.system("code sequence concurrency test")) {
                            return generateService.generate(new GenerateCodeCommand(
                                    rule.getModuleAlias(),
                                    "main",
                                    null,
                                    "orderNo",
                                    null,
                                    LocalDateTime.parse("2026-06-08T10:00:00"),
                                    Map.of(),
                                    null
                            )).value();
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
            assertThat(values).containsExactlyElementsOf(IntStream.rangeClosed(1, count)
                    .mapToObj(i -> "SO-%04d".formatted(i))
                    .toList());
        } finally {
            executor.shutdownNow();
        }
        try (TenantContext.Scope ignored = TenantContext.system("code sequence concurrency test")) {
            assertThat(stateService.selectState(rule.getId(), CodeSequenceState.DEFAULT_BUCKET, CodeSequenceState.DEFAULT_BUCKET)
                    .getCurrentValue()).isEqualTo(count);
        }
    }

    @Test
    void shouldTreatLedgerAsSingleOccupationFact() {
        requirePostgres();
        CodeRule rule = rule(uniqueModuleAlias(), "orderNo");
        ruleService.saveRuleTree(rule);

        ledgerService.upsertActiveBinding(rule, "SO-0001", CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET, "order-1");

        assertThatThrownBy(() -> ledgerService.upsertActiveBinding(rule, "SO-0001",
                CodeSequenceState.DEFAULT_BUCKET, CodeSequenceState.DEFAULT_BUCKET, "order-2"))
                .hasMessageContaining("already occupied");

        ledgerService.upsertInactiveBinding(rule, "SO-0001", CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET, "order-1", CodeLedgerStatus.AVAILABLE,
                CodeLedgerAction.RELEASED_BY_DELETE);
        ledgerService.upsertActiveBinding(rule, "SO-0001", CodeSequenceState.DEFAULT_BUCKET,
                CodeSequenceState.DEFAULT_BUCKET, "order-2");

        CodeLedgerEntry rebound = ledgerService.findByRuleAndValue(rule.getId(), "SO-0001");
        assertThat(rebound.getStatus()).isEqualTo(CodeLedgerStatus.ACTIVE);
        assertThat(rebound.getSourceRecordId()).isEqualTo("order-2");
    }

    @Test
    void shouldConsumeRecycleAtomicallyUnderConcurrentRepositoryWrites() throws Exception {
        requirePostgres();
        CodeRule rule = rule(uniqueModuleAlias(), "orderNo");
        rule.setAllowRecycle(Boolean.TRUE);
        ruleService.saveRuleTree(rule);
        recycleService.record(rule, CodeSequenceState.DEFAULT_BUCKET, CodeSequenceState.DEFAULT_BUCKET,
                "SO-0001", "order-1");

        int count = 12;
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(count);
        try {
            List<Callable<CodeRecycleEntry>> tasks = IntStream.range(0, count)
                    .mapToObj(i -> (Callable<CodeRecycleEntry>) () -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        return recycleService.consumeAvailable(rule.getId(), CodeSequenceState.DEFAULT_BUCKET,
                                CodeSequenceState.DEFAULT_BUCKET);
                    })
                    .toList();
            var futures = tasks.stream().map(executor::submit).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<CodeRecycleEntry> consumed = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    })
                    .filter(entry -> entry != null)
                    .toList();

            assertThat(consumed).hasSize(1);
            assertThat(consumed.getFirst().getRecycledValue()).isEqualTo("SO-0001");
            assertThat(recycleService.list(Criteria.of()
                    .eq("ruleId", rule.getId())
                    .eq("recycledValue", "SO-0001"), PageRequest.of(1, 1))
                    .getFirst()
                    .getStatus()).isEqualTo(CodeRecycleStatus.USED);
        } finally {
            executor.shutdownNow();
        }
    }

    private CodeRule rule(String moduleAlias, String fieldName) {
        CodeRule rule = new CodeRule();
        rule.setModuleAlias(moduleAlias);
        rule.setEntityAlias("main");
        rule.setFieldName(fieldName);
        rule.setTitle(fieldName);
        rule.setFieldRole(CodeFieldRole.PRIMARY);
        rule.setMode(CodeMode.AUTO);
        rule.setEnabled(Boolean.TRUE);
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
        return rule;
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

    private String uniqueModuleAlias() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "crm.code_" + suffix;
    }

    private void requirePostgres() {
        assumeTrue(
                config.getOptionalValue("muyun.test.postgres.enabled", Boolean.class).orElse(false),
                "PostgreSQL integration test is disabled; run with -Pmuyun.postgres.it.required=true to enable it"
        );
    }

    public static class PostgresProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.devservices.enabled", "false");
            config.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/muyun_platform_it");
            config.put("quarkus.datasource.username", "testuser");
            config.put("quarkus.datasource.password", "testpass");
            config.put("muyun.database.default-schema", "public");
            config.put("muyun.database.install-postgres-plugins", "true");
            config.put("muyun.platform-bootstrap.enabled", "false");
            config.put("muyun.platform.time.default-zone-id", "Asia/Shanghai");
            config.put("quarkus.arc.exclude-types", String.join(",",
                    "net.ximatai.muyun.spring.boot.web.CrudWebFormSchemaTest$*",
                    "net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebControllerIT$NoopTenantService",
                    "net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebControllerIT$TestBeans",
                    "net.ximatai.muyun.spring.boot.iam.IamWebControllerIT$TestBeans"
            ));
            config.put("quarkus.arc.remove-unused-beans", "false");
            if (Boolean.getBoolean("muyun.postgres.it.required")) {
                return config;
            }

            config.put("muyun.test.postgres.enabled", "false");
            config.put("muyun.database.repository-schema-mode", "NONE");
            return config;
        }
    }
}
