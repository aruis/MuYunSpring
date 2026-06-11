package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PlatformModuleTaskCheckService {
    private static final PageRequest FIRST_ROW = new PageRequest(1, 1);

    private final PlatformPageConfigSnapshotService snapshotService;
    private final PlatformQueryItemService queryItemService;
    private final DynamicRecordService recordService;

    public PlatformModuleTaskCheckService(PlatformPageConfigSnapshotService snapshotService,
                                          PlatformQueryItemService queryItemService,
                                          DynamicRecordService recordService) {
        this.snapshotService = snapshotService;
        this.queryItemService = queryItemService;
        this.recordService = recordService;
    }

    public List<PlatformModuleTaskStatus> check(String moduleAlias, String recordId, String uiConfigId) {
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
        return blocks.stream()
                .map(block -> checkBlock(snapshot, recordId, block))
                .toList();
    }

    private PlatformModuleTaskStatus checkBlock(PlatformPageConfigSnapshot snapshot,
                                                String recordId,
                                                PlatformTaskBlock block) {
        if (block.checkType() == PlatformTaskCheckType.ASSOCIATION_VIEW) {
            return checkAssociationView(snapshot.moduleAlias(), recordId, block);
        }
        if (block.checkType() == PlatformTaskCheckType.QUERY_TEMPLATE) {
            return checkQueryTemplate(snapshot, recordId, block);
        }
        return new PlatformModuleTaskStatus(block.key(), block.title(), block.checkType(),
                PlatformTaskCompletionStatus.UNKNOWN, null, block.diagnosticPath(),
                "manual task has no backend check");
    }

    private PlatformModuleTaskStatus checkAssociationView(String moduleAlias,
                                                          String recordId,
                                                          PlatformTaskBlock block) {
        String entityAlias = recordService.mainEntityAlias(moduleAlias);
        PageResult<DynamicRecord> page = recordService.associationViewPage(moduleAlias, entityAlias, recordId,
                block.associationViewCode(), Criteria.of(), FIRST_ROW);
        long matched = page.getTotal();
        return new PlatformModuleTaskStatus(block.key(), block.title(), block.checkType(),
                matched > 0 ? PlatformTaskCompletionStatus.COMPLETE : PlatformTaskCompletionStatus.PENDING,
                matched, block.diagnosticPath(), null);
    }

    private PlatformModuleTaskStatus checkQueryTemplate(PlatformPageConfigSnapshot snapshot,
                                                        String recordId,
                                                        PlatformTaskBlock block) {
        PlatformQueryTemplate template = snapshot.queryTemplates().stream()
                .filter(item -> Objects.equals(item.getId(), block.queryTemplateId()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("Task query template is not published in module snapshot: "
                        + block.queryTemplateId()));
        Map<String, Object> externalValues = new LinkedHashMap<>();
        if (hasText(block.externalRecordIdKey())) {
            externalValues.put(block.externalRecordIdKey(), recordId);
        }
        Criteria criteria = queryItemService.compile(template.getId(), externalValues);
        long matched = recordService.count(template.getModuleAlias(),
                recordService.mainEntityAlias(template.getModuleAlias()), criteria);
        return new PlatformModuleTaskStatus(block.key(), block.title(), block.checkType(),
                matched > 0 ? PlatformTaskCompletionStatus.COMPLETE : PlatformTaskCompletionStatus.PENDING,
                matched, block.diagnosticPath(), null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
