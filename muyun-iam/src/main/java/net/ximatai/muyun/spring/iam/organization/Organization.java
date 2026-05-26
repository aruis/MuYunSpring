package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.SortModel;
import net.ximatai.muyun.spring.common.model.StandardBaseModel;
import net.ximatai.muyun.spring.common.model.TitledModel;
import net.ximatai.muyun.spring.common.model.TreeModel;

@Table(name = "iam_organization", comment = "Organization")
public class Organization extends StandardBaseModel implements TreeModel, SortModel, TitledModel {
    @Column(name = "parent_id", type = ColumnType.VARCHAR, length = 32, comment = "Parent organization ID")
    private String parentId;

    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, unique = true, comment = "Organization code")
    private String code;

    @Column(name = "name", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Organization name")
    private String name;

    @Column(name = "sort_order", type = ColumnType.INT, comment = "Sort order")
    private Integer sortOrder;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, comment = "Enabled flag")
    private Boolean enabled;

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Integer getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getTitle() {
        return name;
    }
}
