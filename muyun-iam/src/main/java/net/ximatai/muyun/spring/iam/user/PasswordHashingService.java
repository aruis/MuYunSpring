package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.exception.PlatformException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class PasswordHashingService {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        String validPassword = requirePassword(password);
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(validPassword.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String password, String encoded) {
        if (password == null || encoded == null || encoded.isBlank()) {
            return false;
        }
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
        return java.security.MessageDigest.isEqual(expected, actual);
    }

    private String requirePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new PlatformException("password length must be at least 6");
        }
        return password;
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new PlatformException("password hashing failed");
        }
    }
}
