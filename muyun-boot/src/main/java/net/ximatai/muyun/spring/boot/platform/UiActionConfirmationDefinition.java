package net.ximatai.muyun.spring.boot.platform;

/**
 * Client-side confirmation policy for a module action. This policy improves
 * interaction safety only; it is not part of the action's server-side command contract.
 */
public record UiActionConfirmationDefinition(String requiredField) {
    public UiActionConfirmationDefinition {
        requiredField = requireText(requiredField, "confirmation requiredField");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
