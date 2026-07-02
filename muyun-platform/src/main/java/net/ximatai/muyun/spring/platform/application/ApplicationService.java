package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.StandardBusinessService;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import jakarta.inject.Inject;
import jakarta.enterprise.context.Dependent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryDescriptors;

@Dependent
public class ApplicationService extends StandardBusinessService<Application> implements
        SoftDeleteAbility<Application>,
        EnableAbility<Application>,
        SortAbility<Application>,
        InitialDataAbility<Application>,
        QueryAbility<Application> {

    public static final String MODULE_ALIAS = "platform.application";
    public static final String PLATFORM_APPLICATION_ALIAS = "platform";
    public static final String IAM_APPLICATION_ALIAS = "iam";

    private final Optional<PlatformModuleService> moduleService;
    private final Optional<MetadataService> metadataService;
    private final Optional<DictionaryCategoryService> dictionaryCategoryService;

    public ApplicationService(BaseDao<Application, String> applicationDao) {
        this(applicationDao, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Inject
    public ApplicationService(BaseDao<Application, String> applicationDao,
                              Optional<PlatformModuleService> moduleService,
                              Optional<MetadataService> metadataService,
                              Optional<DictionaryCategoryService> dictionaryCategoryService) {
        super(MODULE_ALIAS, Application.class, applicationDao);
        this.moduleService = moduleService == null ? Optional.empty() : moduleService;
        this.metadataService = metadataService == null ? Optional.empty() : metadataService;
        this.dictionaryCategoryService = dictionaryCategoryService == null ? Optional.empty() : dictionaryCategoryService;
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptors.fromModel(MODULE_ALIAS, Application.class, java.util.List.of("id", "title", "enabled", "sortOrder", "createdAt", "updatedAt"),
                net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"));
    }

    @Override
    public InitialDataOptions initialDataOptions() {
        return InitialDataOptions.system("platform.applications", 10);
    }

    @Override
    public List<Application> initialData() {
        return List.of(
                application(PLATFORM_APPLICATION_ALIAS, "平台能力", 10),
                application(IAM_APPLICATION_ALIAS, "身份权限", 20)
        );
    }

    @Override
    public void normalizeBeforeMutation(Application application) {
        requireAlias(application.getAlias());
    }

    @Override
    public void beforeDelete(String id) {
        String applicationAlias = PlatformNameRules.requireApplicationAlias(id);
        rejectReferenced(moduleService, applicationAlias, "module", "模块");
        rejectReferenced(metadataService, applicationAlias, "metadata", "元数据");
        rejectReferenced(dictionaryCategoryService, applicationAlias, "dictionaryCategory", "字典类目");
    }

    private void requireAlias(String alias) {
        PlatformNameRules.requireApplicationAlias(alias);
    }

    private Application application(String alias, String title, int sortOrder) {
        Application application = new Application();
        application.setAlias(alias);
        application.setTitle(title);
        application.setEnabled(Boolean.TRUE);
        application.setSortOrder(sortOrder);
        return application;
    }

    private <T extends EntityContract> void rejectReferenced(Optional<? extends CrudAbility<T>> service,
                                                             String applicationAlias,
                                                             String resourceKey,
                                                             String resourceName) {
        if (service.map(value -> hasApplicationRecords(value, applicationAlias)).orElse(false)) {
            throw new PlatformException(PlatformErrorCodes.RESOURCE_IN_USE, 409,
                    "该应用下仍有" + resourceName + "，不能删除",
                    ErrorScope.module(MODULE_ALIAS).action("delete"),
                    List.of(ErrorTarget.record(applicationAlias).module(MODULE_ALIAS)),
                    Map.of(
                            "applicationAlias", applicationAlias,
                            "referencedResource", resourceKey));
        }
    }

    private <T extends EntityContract> boolean hasApplicationRecords(CrudAbility<T> service, String applicationAlias) {
        return !service.list(Criteria.of().eq("applicationAlias", applicationAlias), PageRequest.of(1, 1)).isEmpty();
    }
}
