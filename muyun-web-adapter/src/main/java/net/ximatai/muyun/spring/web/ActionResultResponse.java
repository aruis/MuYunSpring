package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.ability.action.DataChange;

import java.util.List;

public record ActionResultResponse(
        Object data,
        ActionMessage message,
        String changeSetId,
        List<DataChange> changes
) {
    public ActionResultResponse {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }
}
