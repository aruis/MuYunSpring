package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PositionService extends TenantStandardBusinessService<Position> implements
        SoftDeleteAbility<Position>,
        EnableAbility<Position>,
        SortAbility<Position>,
        ReferenceAbility<Position> {
    public static final String MODULE_ALIAS = "iam.position";

    @Autowired
    public PositionService(PositionDao positionDao, ActiveTenantVerifier activeTenantVerifier) {
        super(MODULE_ALIAS, Position.class, positionDao, activeTenantVerifier);
    }

    @Override
    public void normalizeBeforeMutation(Position position) {
        position.setCode(Preconditions.requireText(position.getCode(), "positionCode"));
        position.setTitle(Preconditions.requireText(position.getTitle(), "positionTitle"));
        position.setDescription(normalizeBlank(position.getDescription()));
    }

    @Override
    protected void validateBeforeSave(Position position) {
        rejectDuplicate(position, Criteria.of().eq("code", position.getCode()),
                "positionCode must be unique within tenant: " + position.getCode());
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
