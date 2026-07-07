package net.ximatai.muyun.spring.boot.iam;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeResolver;
import net.ximatai.muyun.spring.boot.web.ScopedTreeWeb;
import net.ximatai.muyun.spring.boot.web.TreeScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.department", title = "部门管理", route = "/iam/departments")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, title = "部门管理", order = 30)
@RequestMapping("/iam.department")
public class DepartmentWebController extends WebSupport<DepartmentService> implements
        CrudWeb<Department, DepartmentService>,
        EnableWeb<Department, DepartmentService>,
        ScopedTreeWeb<Department, DepartmentService>,
        MutationTenantScopeResolver<Department>,
        StaticModuleUiContributor {
    private OrganizationService organizationService;
    private StaticRecordReadProjectionService staticRecordReadProjectionService;

    @Autowired
    void setOrganizationService(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Autowired(required = false)
    void setStaticRecordReadProjectionService(StaticRecordReadProjectionService staticRecordReadProjectionService) {
        this.staticRecordReadProjectionService = staticRecordReadProjectionService;
    }

    @Override
    public StaticRecordReadProjectionService staticRecordReadProjectionService() {
        return staticRecordReadProjectionService;
    }

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(DepartmentService.MODULE_ALIAS)
                .listView(list -> list
                        .title("部门列表")
                        .field("code", field -> field.label("部门编码").width("150px"))
                        .field("title", field -> field.label("部门名称").width("180px"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus")
                                .width("90px").align("center")))
                .formView(form -> form
                        .title("部门档案")
                        .field("organizationId", field -> field.label("所属机构").required().readOnly())
                        .field("parentId", field -> field.label("上级部门").uiType("recordPicker"))
                        .field("code", field -> field.label("部门编码").required())
                        .field("title", field -> field.label("部门名称").required())
                        .field("enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                .build();
    }

    @Override
    public TreeScope treeScope(HttpServletRequest request) {
        return departmentTreeScope(request.getParameter("organizationId"));
    }

    @Override
    public TreeScope treeScope(HttpServletRequest request, Department record) {
        return departmentTreeScope(record.getOrganizationId());
    }

    @Override
    public TreeScope treeScopeForRecordLookup(HttpServletRequest request, String id) {
        String organizationId = request.getParameter("organizationId");
        return organizationId == null || organizationId.isBlank()
                ? TreeScope.none()
                : departmentTreeScope(organizationId);
    }

    @Override
    public Optional<String> tenantIdForCreate(Department record) {
        return tenantIdForOrganization(record == null ? null : record.getOrganizationId());
    }

    @Override
    public Optional<String> tenantIdForUpdate(String id, Department record) {
        Department existing = service().select(id);
        if (existing != null) {
            return tenantIdForOrganization(existing.getOrganizationId());
        }
        return tenantIdForCreate(record);
    }

    @Override
    public Optional<String> tenantIdForExistingRecord(String id) {
        Department existing = service().select(id);
        return tenantIdForOrganization(existing == null ? null : existing.getOrganizationId());
    }

    private TreeScope departmentTreeScope(String organizationId) {
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        return TreeScope.of(Criteria.of().eq("organizationId", validOrganizationId));
    }

    private Optional<String> tenantIdForOrganization(String organizationId) {
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        Organization organization = organizationService.requireEnabled(validOrganizationId,
                "organization is not active: " + validOrganizationId);
        return Optional.of(Preconditions.requireText(organization.getTenantId(), "organization.tenantId"));
    }
}
