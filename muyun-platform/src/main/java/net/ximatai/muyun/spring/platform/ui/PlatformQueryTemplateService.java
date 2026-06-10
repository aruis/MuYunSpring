package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PlatformQueryTemplateService extends AbstractAbilityService<PlatformQueryTemplate> implements
        SoftDeleteAbility<PlatformQueryTemplate>,
        EnableAbility<PlatformQueryTemplate>,
        SortAbility<PlatformQueryTemplate> {
    public static final String MODULE_ALIAS = "platform.queryTemplate";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformModuleService moduleService;

    public PlatformQueryTemplateService(BaseDao<PlatformQueryTemplate, String> queryTemplateDao,
                                        PlatformModuleService moduleService) {
        super(MODULE_ALIAS, PlatformQueryTemplate.class, queryTemplateDao);
        this.moduleService = moduleService;
    }

    @Override
    public void beforeInsert(PlatformQueryTemplate template) {
        normalizeAndValidate(template);
        rejectDirectPublish(template);
    }

    @Override
    public void beforeUpdate(PlatformQueryTemplate template) {
        PlatformQueryTemplate existing = selectIncludingDeleted(template.getId());
        rejectDirectPublish(existing, template);
        rejectPublishedEdit(existing, template);
        normalizeAndValidate(template);
        rejectChanged(existing, template, "Query template moduleAlias", PlatformQueryTemplate::getModuleAlias);
        rejectChanged(existing, template, "Query template alias", PlatformQueryTemplate::getAlias);
    }

    @Override
    public void beforeDelete(String id) {
        PlatformQueryTemplate existing = select(id);
        if (existing != null && Boolean.TRUE.equals(existing.getPublished())) {
            throw new PlatformException("Published query template cannot be deleted; unpublish first: " + id);
        }
    }

    @Override
    public Criteria sortScope(PlatformQueryTemplate template) {
        return Criteria.of().eq("moduleAlias", template.getModuleAlias());
    }

    @Override
    public void validateSortScope(PlatformQueryTemplate left, PlatformQueryTemplate right) {
        if (!Objects.equals(left.getModuleAlias(), right.getModuleAlias())) {
            throw new PlatformException("Query template sort can only move records within the same module");
        }
    }

    public PlatformQueryTemplate requireQueryTemplate(String id) {
        PlatformQueryTemplate template = id == null || id.isBlank() ? null : select(id);
        if (template == null) {
            throw new PlatformException("Query template requires existing config: " + id);
        }
        return template;
    }

    public List<PlatformQueryTemplate> listByModule(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        return list(enabledCriteria(Criteria.of().eq("moduleAlias", validAlias)),
                ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<PlatformQueryTemplate> listPublishedByModule(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        return list(enabledCriteria(Criteria.of()
                        .eq("moduleAlias", validAlias)
                        .eq("published", Boolean.TRUE)),
                ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    private void normalizeAndValidate(PlatformQueryTemplate template) {
        String moduleAlias = PlatformNameRules.requireModuleAlias(template.getModuleAlias());
        PlatformModule module = moduleService.resolveVisibleModule(moduleAlias);
        if (module == null) {
            throw new PlatformException("Query template requires existing module: " + moduleAlias);
        }
        String alias = PlatformNameRules.requireIdentifier(template.getAlias(), "queryTemplateAlias");
        template.setModuleAlias(moduleAlias);
        template.setAlias(alias);
        if (template.getTitle() == null || template.getTitle().isBlank()) {
            template.setTitle(alias);
        }
        if (template.getDefaultTemplate() == null) {
            template.setDefaultTemplate(Boolean.FALSE);
        }
        if (template.getPublished() == null) {
            template.setPublished(Boolean.FALSE);
        }
        rejectDuplicate(template, Criteria.of()
                        .eq("moduleAlias", moduleAlias)
                        .eq("alias", alias),
                "Query template alias must be unique in module: " + moduleAlias + "." + alias);
        if (Boolean.TRUE.equals(template.getDefaultTemplate())) {
            rejectDuplicate(template, Criteria.of()
                            .eq("moduleAlias", moduleAlias)
                            .eq("defaultTemplate", Boolean.TRUE),
                    "Only one default query template is allowed for module: " + moduleAlias);
        }
    }

    private void rejectPublishedEdit(PlatformQueryTemplate existing, PlatformQueryTemplate updated) {
        if (existing == null || !Boolean.TRUE.equals(existing.getPublished())) {
            return;
        }
        if (Boolean.FALSE.equals(updated.getPublished())
                && Objects.equals(existing.getModuleAlias(), updated.getModuleAlias())
                && Objects.equals(existing.getAlias(), updated.getAlias())
                && Objects.equals(existing.getDefaultTemplate(), updated.getDefaultTemplate())
                && Objects.equals(existing.getTitle(), updated.getTitle())
                && Objects.equals(existing.getEnabled(), updated.getEnabled())
                && Objects.equals(existing.getSortOrder(), updated.getSortOrder())) {
            return;
        }
        throw new PlatformException("Published query template cannot be edited; unpublish first: " + existing.getId());
    }

    private void rejectDirectPublish(PlatformQueryTemplate template) {
        if (Boolean.TRUE.equals(template.getPublished()) && !PlatformPageConfigPublishContext.active()) {
            throw new PlatformException("Query template can only be published through publish service: "
                    + template.getId());
        }
    }

    private void rejectDirectPublish(PlatformQueryTemplate existing, PlatformQueryTemplate updated) {
        if (PlatformPageConfigPublishContext.active() || updated == null || !Boolean.TRUE.equals(updated.getPublished())) {
            return;
        }
        if (existing == null || !Boolean.TRUE.equals(existing.getPublished())) {
            throw new PlatformException("Query template can only be published through publish service: "
                    + updated.getId());
        }
    }
}
