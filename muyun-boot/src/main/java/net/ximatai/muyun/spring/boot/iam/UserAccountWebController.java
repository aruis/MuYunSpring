package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.user", title = "用户管理")
@RequestMapping("/iam.user")
public class UserAccountWebController extends WebSupport<UserAccountService> implements
        CrudWeb<UserAccount, UserAccountService>,
        EnableWeb<UserAccount, UserAccountService>,
        SortWeb<UserAccount, UserAccountService> {
    private static final ActionExecutionPolicy USER_SELECTOR_POLICY = new ActionExecutionPolicy(
            "userSelector",
            PlatformActionLevel.LIST,
            ActionAccessMode.AUTH_REQUIRED,
            true,
            true,
            ActionDefaultGrantPolicy.NONE,
            null
    );

    private final UserSessionService userSessionService;
    private final RoleService roleService;

    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService) {
        this(userSessionService, null);
    }

    @Autowired
    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService,
                                    ObjectProvider<RoleService> roleService) {
        this.userSessionService = userSessionService == null ? null : userSessionService.getIfAvailable();
        this.roleService = roleService == null ? null : roleService.getIfAvailable();
    }

    @PostMapping("/changePassword/{id}")
    @CustomActionEndpoint(value = "changePassword", title = "修改密码",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WebCountResponse changePassword(@PathVariable String id,
                                           @RequestBody ChangePasswordRequest request) {
        return webScope(() -> {
            int changed = service().changePassword(id, request.password());
            if (changed > 0 && userSessionService != null) {
                userSessionService.revokeUserSessions(id);
            }
            return new WebCountResponse(changed);
        });
    }

    @PostMapping("/selector/query")
    @CustomActionEndpoint(value = "userSelector", title = "用户选择器", level = PlatformActionLevel.LIST,
            dataAuth = true)
    public WebPageResponse<UserSelectorItem> selector(@RequestBody(required = false) UserSelectorRequest request) {
        return webScope(() -> {
            UserSelectorRequest normalized = request == null ? UserSelectorRequest.EMPTY : request;
            Criteria criteria = selectorCriteria(normalized);
            WebPageRequest page = normalized.pageOrDefault();
            PageResult<UserAccount> result = selectorPageQuery(criteria,
                    PageRequest.of(page.pageNum(), page.pageSize()), Sort.asc("username"));
            return WebPageResponse.from(PageResult.of(
                    result.getRecords().stream().map(UserSelectorItem::from).toList(),
                    result.getTotal(),
                    PageRequest.of(result.getPageNum(), result.getPageSize())
            ));
        });
    }

    public record ChangePasswordRequest(String password) {
    }

    public record UserSelectorRequest(
            String organizationId,
            String roleId,
            String keyword,
            Boolean enabledOnly,
            WebPageRequest page
    ) {
        static final UserSelectorRequest EMPTY = new UserSelectorRequest(null, null, null, Boolean.TRUE, null);

        WebPageRequest pageOrDefault() {
            return page == null ? WebPageRequest.DEFAULT : page;
        }
    }

    public record UserSelectorItem(
            String id,
            String username,
            String title,
            String organizationId,
            String mobile,
            String email
    ) {
        static UserSelectorItem from(UserAccount user) {
            return new UserSelectorItem(
                    user.getId(),
                    user.getUsername(),
                    user.getTitle(),
                    user.getOrganizationId(),
                    user.getMobile(),
                    user.getEmail()
            );
        }
    }

    private Criteria selectorCriteria(UserSelectorRequest request) {
        Criteria criteria = Criteria.of();
        if (!Boolean.FALSE.equals(request.enabledOnly())) {
            criteria.eq("enabled", Boolean.TRUE);
        }
        if (request.organizationId() != null && !request.organizationId().isBlank()) {
            criteria.eq("organizationId", request.organizationId().trim());
        }
        if (request.roleId() != null && !request.roleId().isBlank()) {
            if (roleService == null) {
                throw new IllegalStateException("role service is not available");
            }
            java.util.List<String> userIds = roleService.userIds(request.roleId());
            if (userIds.isEmpty()) {
                criteria.in("id", java.util.List.of("__none__"));
            } else {
                criteria.in("id", userIds);
            }
        }
        if (request.keyword() != null && !request.keyword().isBlank()) {
            String keyword = request.keyword().trim();
            Criteria keywordCriteria = Criteria.of();
            keywordCriteria.orGroup(Criteria.of().like("username", keyword).getRoot());
            keywordCriteria.orGroup(Criteria.of().like("mobile", keyword).getRoot());
            keywordCriteria.orGroup(Criteria.of().like("email", keyword).getRoot());
            criteria.andGroup(keywordCriteria.getRoot());
        }
        return criteria;
    }

    private PageResult<UserAccount> selectorPageQuery(Criteria criteria, PageRequest pageRequest, Sort sort) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<UserAccount> dataScopeAbility = DataScopeAbility.cast(service());
            DataScopeCriteriaResult scope = dataScopeAbility.readScopeByPolicy(selectorPolicy(), criteria);
            return dataScopeAbility.withDataScopeTenant(scope,
                    () -> service().pageQuery(scope.criteria(), pageRequest, sort));
        }
        return service().pageQuery(criteria, pageRequest, sort);
    }

    private ActionExecutionPolicy selectorPolicy() {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElse(USER_SELECTOR_POLICY);
    }
}
