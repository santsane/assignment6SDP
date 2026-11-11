package com.inspection.visitor;

import java.util.List;
import java.util.Locale;

/**
 * Small shared helper so the security visitor and the compliance visitor
 * agree on what an "exposed credential" in a config file looks like.
 */
final class SecretRules {

    private static final List<String> SECRET_WORDS =
            List.of("password", "passwd", "secret", "token", "apikey", "api_key", "api-key");

    private SecretRules() {
    }

    /** true if the key looks like a secret AND the value is a real hardcoded value. */
    static boolean isExposedSecret(String key, String value) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        boolean looksLikeSecret = SECRET_WORDS.stream().anyMatch(lowerKey::contains);
        if (!looksLikeSecret) {
            return false;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        // ${DB_PASSWORD} means it is read from the environment, that is fine
        return !value.trim().startsWith("${");
    }
}
