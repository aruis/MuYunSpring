package net.ximatai.muyun.spring.platform.impact;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicMutationContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class RecordImpactOriginCoordinator implements DynamicRecordMutationCoordinator {
    public static final String ORIGIN_CONTEXT_KEY = "originContext";

    private final RecordImpactRelationService relationService;

    public RecordImpactOriginCoordinator(RecordImpactRelationService relationService) {
        this.relationService = Objects.requireNonNull(relationService, "relationService must not be null");
    }

    @Override
    public void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
        register(moduleAlias, id);
    }

    @Override
    public void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
        register(moduleAlias, updated == null ? null : updated.getId());
    }

    private void register(String targetModuleAlias, String targetRecordId) {
        RecordOriginContext context = originContext();
        if (context == null) {
            return;
        }
        relationService.registerFromOriginContext(
                context,
                targetModuleAlias,
                targetRecordId,
                CurrentUserContext.currentUser().map(user -> user.userId()).orElse(null));
    }

    private RecordOriginContext originContext() {
        return DynamicMutationContext.current()
                .map(context -> context.metadata(ORIGIN_CONTEXT_KEY))
                .map(this::originContext)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private RecordOriginContext originContext(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof RecordOriginContext context) {
            return context;
        }
        if (value instanceof Map<?, ?> map) {
            return originContext((Map<String, Object>) map);
        }
        throw new PlatformException("Record origin context must be object");
    }

    private RecordOriginContext originContext(Map<String, Object> values) {
        return new RecordOriginContext(
                impactType(values.get("impactType")),
                text(values.get("sourceModuleAlias")),
                text(values.get("sourceRecordId")),
                text(values.get("targetModuleAlias")),
                text(values.get("generationRuleId")),
                text(values.get("actionCode")),
                text(values.get("batchId")),
                text(values.get("draftKey"))
        );
    }

    private RecordImpactType impactType(Object value) {
        if (value instanceof RecordImpactType impactType) {
            return impactType;
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        return RecordImpactType.valueOf(text.trim().toUpperCase(Locale.ROOT));
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }
}
