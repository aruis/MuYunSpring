package net.ximatai.muyun.spring.ability.reference;

/** A source-independent typed reference projection path. */
public record ReferenceLoadPath(
        String sourceField,
        ReferenceTarget sourceTarget,
        java.util.List<Hop> hops,
        String terminalField,
        String outputField
) {
    public ReferenceLoadPath {
        hops = hops == null ? java.util.List.of() : java.util.List.copyOf(hops);
    }

    public record Hop(ReferenceTarget target, String viaField) {
        public Hop {
            viaField = viaField == null || viaField.isBlank() ? null : viaField.trim();
        }
    }
}
