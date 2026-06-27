package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.StandardBusinessService;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ApplicationService extends StandardBusinessService<Application> implements
        SoftDeleteAbility<Application>,
        EnableAbility<Application>,
        SortAbility<Application>,
        QueryAbility<Application> {

    public static final String MODULE_ALIAS = "platform.application";

    private final Optional<PlatformModuleService> moduleService;
    private final Optional<MetadataService> metadataService;
    private final Optional<DictionaryCategoryService> dictionaryCategoryService;

    public ApplicationService(BaseDao<Application, String> applicationDao) {
        this(applicationDao, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Autowired
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
    public void normalizeBeforeMutation(Application application) {
        requireAlias(application.getAlias());
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("应用名称").withQuickSearch().withSortable())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .build();
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
