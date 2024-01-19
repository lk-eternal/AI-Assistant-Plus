package lk.eternal.ai.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private static Pbkdf2PasswordEncoder PBKDF2_PASSWORD_ENCODER;

    public PasswordUtil(@Value("${app.security.password.secret}") String secret) {
        PBKDF2_PASSWORD_ENCODER = new Pbkdf2PasswordEncoder(secret, 10, 1000, Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    }

    public static String hashPassword(String rawPassword) {
        return PBKDF2_PASSWORD_ENCODER.encode(rawPassword);
    }

    public static boolean validatePassword(String rawPassword, String encodedPassword) {
        return PBKDF2_PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}