package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionRecoveryAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.StandardBusinessService;
import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.TenantApplicationCatalog;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryDescriptors;

@Service
public class ApplicationService extends StandardBusinessService<Application> implements
        RecycleBinAbility<Application>,
        DeletionRecoveryAbility<Application>,
        GlobalScopedAbility<Application>,
        EnableAbility<Application>,
        SortAbility<Application>,
        QueryAbility<Application>,
        TenantApplicationCatalog {

    public static final String MODULE_ALIAS = "platform.application";
    public static final String PLATFORM_APPLICATION_ALIAS = "platform";
    public static final String IAM_APPLICATION_ALIAS = "iam";

    private final Supplier<List<ApplicationReferenceContributor>> referenceContributors;

    public ApplicationService(BaseDao<Application, String> applicationDao) {
        this(applicationDao, List.of());
    }

    public ApplicationService(BaseDao<Application, String> applicationDao,
                              List<ApplicationReferenceContributor> referenceContributors) {
        super(MODULE_ALIAS, Application.class, applicationDao);
        List<ApplicationReferenceContributor> stableContributors = referenceContributors == null
                ? List.of()
                : List.copyOf(referenceContributors);
        this.referenceContributors = () -> stableContributors;
    }

    @Autowired
    public ApplicationService(BaseDao<Application, String> applicationDao,
                              ObjectProvider<ApplicationReferenceContributor> referenceContributors) {
        super(MODULE_ALIAS, Application.class, applicationDao);
        this.referenceContributors = () -> referenceContributors.orderedStream().toList();
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptors.fromModel(MODULE_ALIAS, Application.class, java.util.List.of("id", "title", "enabled", "sortOrder", "createdAt", "updatedAt"),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"));
    }

    @Override
    public void normalizeBeforeMutation(Application application) {
        requireAlias(application.getAlias());
    }

    @Override
    public boolean isEnabledForTenant(String applicationAlias) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        Application application = select(validApplicationAlias);
        return application != null
                && Boolean.TRUE.equals(application.getEnabled())
                && !PLATFORM_APPLICATION_ALIAS.equals(validApplicationAlias);
    }

    @Override
    public void requireEnabledForTenant(String applicationAlias) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        if (PLATFORM_APPLICATION_ALIAS.equals(validApplicationAlias)) {
            throw new IllegalArgumentException("system application cannot be opened for a tenant: "
                    + validApplicationAlias);
        }
        if (!isEnabledForTenant(validApplicationAlias)) {
            throw new IllegalArgumentException("application is not active: " + validApplicationAlias);
        }
    }

    @Override
    public void beforeDelete(String id) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(id);
        referenceContributors.get().stream()
                .filter(contributor -> contributor.hasReferenceTo(applicationAlias))
                .sorted(Comparator.comparing(ApplicationReferenceContributor::resourceKey))
                .findFirst()
                .ifPresent(contributor -> rejectReferenced(contributor, applicationAlias));
    }

    private void requireAlias(String alias) {
        PlatformNameRules.requireApplicationAlias(alias);
    }

    private void rejectReferenced(ApplicationReferenceContributor contributor, String applicationAlias) {
        throw new PlatformException(PlatformErrorCodes.RESOURCE_IN_USE, 409,
                "该应用下仍有" + contributor.resourceName() + "，不能删除",
                ErrorScope.module(MODULE_ALIAS).action("delete"),
                List.of(ErrorTarget.record(applicationAlias).module(MODULE_ALIAS)),
                Map.of(
                        "applicationAlias", applicationAlias,
                        "referencedResource", contributor.resourceKey()));
    }
}
