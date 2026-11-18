package org.linkwave.userservice.service;

import java.security.SecureRandom;
import java.util.Locale;

/** Sinh mật khẩu tạm: 12 ký tự, có chữ và số, bỏ các ký tự dễ nhầm (0, O, 1, l, I), không chứa tên đăng nhập. */
public final class TemporaryPasswords {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {
    }

    public static String generate(String username) {
        final String loweredName = username.toLowerCase(Locale.ROOT);
        while (true) {
            final StringBuilder sb = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            final String candidate = sb.toString();
            if (candidate.matches(".*[A-Za-z].*")
                    && candidate.matches(".*\\d.*")
                    && !candidate.toLowerCase(Locale.ROOT).contains(loweredName)) {
                return candidate;
            }
        }
    }
}