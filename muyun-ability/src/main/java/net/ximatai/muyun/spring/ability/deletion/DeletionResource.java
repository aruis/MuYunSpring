package net.ximatai.muyun.spring.ability.deletion;

import java.util.Objects;

/** Stable platform resource identity inside one deletion operation. */
public record DeletionResource(String moduleAlias, String recordId) {
    public DeletionResource {
        if (moduleAlias == null || moduleAlias.isBlank()) {
            throw new IllegalArgumentException("moduleAlias must not be blank");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("recordId must not be blank");
        }
    }

    public boolean sameResource(DeletionResource other) {
        return Objects.equals(this, other);
    }
}
