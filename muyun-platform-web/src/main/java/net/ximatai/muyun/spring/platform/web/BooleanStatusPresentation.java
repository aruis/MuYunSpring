package net.ximatai.muyun.spring.platform.web;

/** Presentation metadata for a business boolean value. */
public record BooleanStatusPresentation(String trueLabel,
                                        String falseLabel,
                                        BooleanStatusTone trueTone,
                                        BooleanStatusTone falseTone) {
    public BooleanStatusPresentation {
        trueLabel = requireLabel(trueLabel, "trueLabel");
        falseLabel = requireLabel(falseLabel, "falseLabel");
        trueTone = trueTone == null ? BooleanStatusTone.SUCCESS : trueTone;
        falseTone = falseTone == null ? BooleanStatusTone.NEUTRAL : falseTone;
    }

    private static String requireLabel(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
