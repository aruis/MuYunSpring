package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MetadataActionService extends AbstractAbilityService<MetadataAction> implements
        SoftDeleteAbility<MetadataAction>,
        EnableAbility<MetadataAction>,
        SortAbility<MetadataAction> {
    public static final String MODULE_ALIAS = "platform.metadata_action";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final ModuleMetadataRelationService relationService;

    public MetadataActionService(BaseDao<MetadataAction, String> actionDao,
                                 ModuleMetadataRelationService relationService) {
        super(MODULE_ALIAS, MetadataAction.class, actionDao);
        this.relationService = relationService;
    }

    @Override
    public void beforeInsert(MetadataAction action) {
        normalizeAndValidate(action);
    }

    @Override
    public void beforeUpdate(MetadataAction action) {
        normalizeAndValidate(action);
    }

    @Override
    public Criteria sortScope(MetadataAction action) {
        return Criteria.of().eq("relationId", action.getRelationId());
    }

    @Override
    public void validateSortScope(MetadataAction left, MetadataAction right) {
        if (!Objects.equals(left.getRelationId(), right.getRelationId())) {
            throw new PlatformException("Metadata action sort can only move records within the same relation");
        }
    }

    public List<MetadataAction> listByRelationIds(List<String> relationIds) {
        if (relationIds == null || relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc("sortOrder"));
    }

    private void normalizeAndValidate(MetadataAction action) {
        ModuleMetadataRelation relation = action.getRelationId() == null || action.getRelationId().isBlank()
                ? null
                : relationService.select(action.getRelationId());
        if (relation == null) {
            throw new PlatformException("Metadata action requires existing relation: " + action.getRelationId());
        }
        if (action.getActionCode() == null || !action.getActionCode().matches("[a-z][A-Za-z0-9]{0,63}")) {
            throw new PlatformException("Metadata action requires valid actionCode: " + action.getActionCode());
        }
        if (action.getActionKind() == null) {
            throw new PlatformException("Metadata action requires actionKind");
        }
        if (action.getActionLevel() == null) {
            action.setActionLevel(EntityActionLevel.NORMAL);
        }
        if (action.getTitle() == null || action.getTitle().isBlank()) {
            action.setTitle(action.getActionCode());
        }
        if (action.getPermissionCode() != null && action.getPermissionCode().isBlank()) {
            action.setPermissionCode(null);
        }
        rejectDuplicate(action, Criteria.of()
                        .eq("relationId", action.getRelationId())
                        .eq("actionCode", action.getActionCode()),
                "metadata action must be unique in relation: " + action.getRelationId() + "." + action.getActionCode());
    }
}
