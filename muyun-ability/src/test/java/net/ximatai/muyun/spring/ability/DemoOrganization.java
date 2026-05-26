package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.SortModel;
import net.ximatai.muyun.spring.common.model.StandardBaseModel;
import net.ximatai.muyun.spring.common.model.TitledModel;
import net.ximatai.muyun.spring.common.model.TreeModel;

final class DemoOrganization extends StandardBaseModel implements TreeModel, SortModel, TitledModel {
    private String parentId;
    private String name;
    private Integer sortOrder;

    DemoOrganization(String name, String parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public Integer getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String getTitle() {
        return name;
    }
}
