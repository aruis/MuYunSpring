package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelation;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class PlatformModuleTaskCheckService {
    private static final PageRequest FIRST_ROW = new PageRequest(1, 1);
    private static final int GENERATED_RELATION_PAGE_SIZE = 500;

    private final PlatformPageConfigSnapshotService snapshotService;
    private final PlatformQueryItemService queryItemService;
    private final DynamicRecordService recordService;
    private final Optional<RecordImpactRelationService> impactRelationService;

    public PlatformModuleTaskCheckService(PlatformPageConfigSnapshotService snapshotService,
                                          PlatformQueryItemService queryItemService,
                                          DynamicRecordService recordService,
                                          Optional<RecordImpactRelationService> impactRelationService) {
        this.snapshotService = snapshotService;
        this.queryItemService = queryItemService;
        this.recordService = recordService;
        this.impactRelationService = impactRelationService == null ? Optional.empty() : impactRelationService;
    }

    PlatformModuleTaskCheckService(PlatformPageConfigSnapshotService snapshotService,
                                   PlatformQueryItemService queryItemService,
                                   DynamicRecordService recordService) {
        this(snapshotService, queryItemService, recordService, Optional.empty());
    }

    public PlatformModuleTaskCheckResult check(String moduleAlias, String recordId, String uiConfigId) {
        if (recordId == null || recordId.isBlank()) {
            throw new PlatformException("Module task check requires record id");
        }
        PlatformPageConfigSnapshot snapshot = snapshotService.snapshot(moduleAlias);
        List<PlatformTaskBlock> blocks = snapshot.uiConfigs().stream()
                .filter(config -> uiConfigId == null || uiConfigId.isBlank()
                        || Objects.equals(config.getId(), uiConfigId))
                .flatMap(config -> PlatformTaskBlockLayoutResolver.resolve(config).stream())
                .toList();
        if (hasText(uiConfigId) && blocks.isEmpty()) {
            boolean configExists = snapshot.uiConfigs().stream()
                    .anyMatch(config -> Objects.equals(config.getId(), uiConfigId));
            if (!configExists) {
                throw new PlatformException("UI config is not published in module snapshot: " + uiConfigId);
            }
        }
        return PlatformModuleTaskCheckResult.of(blocks.stream()
                .map(block -> checkBlock(snapshot, recordId, block))
                .toList());
    }

    private PlatformModuleTaskStatus checkBlock(PlatformPageConfigSnapshot snapshot,
                                                String recordId,
                                                PlatformTaskBlock block) {
        List<PlatformModuleTaskCheckDetail> checks = block.checks().stream()
                .map(check -> checkOne(snapshot, recordId, check))
                .toList();
        if (checks.isEmpty() || checks.stream().allMatch(check -> check.passed() == null)) {
            return status(block, PlatformTaskCompletionStatus.UNKNOWN, null, null, null, checks,
                    "manual task has no backend check");
        }
        boolean hasFailed = checks.stream().anyMatch(check -> Boolean.FALSE.equals(check.passed()));
        if (hasFailed) {
            return status(block, PlatformTaskCompletionStatus.PENDING, false, matchedCount(checks),
                    expectedCount(checks), checks, null);
        }
        boolean hasUnknown = checks.stream().anyMatch(check -> check.passed() == null);
        return status(block, hasUnknown ? PlatformTaskCompletionStatus.UNKNOWN : PlatformTaskCompletionStatus.COMPLETE,
                hasUnknown ? null : true, matchedCount(checks), expectedCount(checks), checks, null);
    }

    private PlatformModuleTaskCheckDetail checkOne(PlatformPageConfigSnapshot snapshot,
                                                   String recordId,
                                                   PlatformTaskCheckBlock check) {
        if (check.checkType() == PlatformTaskCheckType.ASSOCIATION_VIEW) {
            return checkAssociationView(snapshot.moduleAlias(), recordId, check);
        }
        if (check.checkType() == PlatformTaskCheckType.QUERY_TEMPLATE) {
            return checkQueryTemplate(snapshot, recordId, check);
        }
        if (check.checkType() == PlatformTaskCheckType.GENERATED_RELATION) {
            return checkGeneratedRelation(snapshot.moduleAlias(), recordId, check);
        }
        return new PlatformModuleTaskCheckDetail(check.checkType(), null, null, check.expectedCount(),
                check.diagnosticPath(), "manual task has no backend check");
    }

    private PlatformModuleTaskCheckDetail checkAssociationView(String moduleAlias,
                                                               String recordId,
                                                               PlatformTaskCheckBlock check) {
        String entityAlias = recordService.mainEntityAlias(moduleAlias);
        PageResult<DynamicRecord> page = recordService.associationViewPage(moduleAlias, entityAlias, recordId,
                check.associationViewCode(), Criteria.of(), FIRST_ROW);
        long matched = page.getTotal();
        return countDetail(check, matched);
    }

    private PlatformModuleTaskCheckDetail checkQueryTemplate(PlatformPageConfigSnapshot snapshot,
                                                             String recordId,
                                                             PlatformTaskCheckBlock check) {
        PlatformQueryTemplate template = snapshot.queryTemplates().stream()
                .filter(item -> Objects.equals(item.getId(), check.queryTemplateId()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("Task query template is not published in module snapshot: "
                        + check.queryTemplateId()));
        Map<String, Object> externalValues = new LinkedHashMap<>();
        if (hasText(check.externalRecordIdKey())) {
            externalValues.put(check.externalRecordIdKey(), recordId);
        }
        Criteria criteria = queryItemService.compile(template.getId(), externalValues);
        long matched = recordService.count(template.getModuleAlias(),
                recordService.mainEntityAlias(template.getModuleAlias()), criteria);
        return countDetail(check, matched);
    }

    private PlatformModuleTaskCheckDetail checkGeneratedRelation(String moduleAlias,
                                                                 String recordId,
                                                                 PlatformTaskCheckBlock check) {
        RecordImpactRelationService service = impactRelationService
                .orElseThrow(() -> new PlatformException("Generated relation task check requires impact relation service"));
        long matched = countVisibleGeneratedTargets(service, moduleAlias, recordId, check);
        return countDetail(check, matched);
    }

    private long countVisibleGeneratedTargets(RecordImpactRelationService service,
                                              String moduleAlias,
                                              String recordId,
                                              PlatformTaskCheckBlock check) {
        String targetEntityAlias = recordService.mainEntityAlias(check.targetModuleAlias());
        Set<String> seenTargetIds = new LinkedHashSet<>();
        long visible = 0;
        int page = 1;
        while (visible < check.expectedCount()) {
            List<RecordImpactRelation> relations = service.listGeneratedTargets(moduleAlias, recordId,
                    check.targetModuleAlias(), check.generationRuleId(),
                    PageRequest.of(page, GENERATED_RELATION_PAGE_SIZE));
            if (relations.isEmpty()) {
                break;
            }
            List<String> candidateIds = relations.stream()
                    .map(RecordImpactRelation::getTargetRecordId)
                    .filter(Objects::nonNull)
                    .filter(seenTargetIds::add)
                    .toList();
            if (!candidateIds.isEmpty()) {
                visible += recordService.count(check.targetModuleAlias(), targetEntityAlias,
                        Criteria.of().in("id", candidateIds));
            }
            if (relations.size() < GENERATED_RELATION_PAGE_SIZE) {
                break;
            }
            page++;
        }
        return visible;
    }

    private PlatformModuleTaskCheckDetail countDetail(PlatformTaskCheckBlock check, long matched) {
        return new PlatformModuleTaskCheckDetail(check.checkType(), matched >= check.expectedCount(), matched,
                check.expectedCount(), check.diagnosticPath(), null);
    }

    private PlatformModuleTaskStatus status(PlatformTaskBlock block,
                                            PlatformTaskCompletionStatus status,
                                            Boolean passed,
                                            Long matchedCount,
                                            Integer expectedCount,
                                            List<PlatformModuleTaskCheckDetail> checks,
                                            String message) {
        return new PlatformModuleTaskStatus(block.key(), block.title(), block.checkType(), status, passed,
                matchedCount, expectedCount, checks, block.diagnosticPath(), message);
    }

    private Long matchedCount(List<PlatformModuleTaskCheckDetail> checks) {
        return checks.stream()
                .map(PlatformModuleTaskCheckDetail::actualCount)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
    }

    private Integer expectedCount(List<PlatformModuleTaskCheckDetail> checks) {
        return checks.stream()
                .map(PlatformModuleTaskCheckDetail::expectedCount)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
