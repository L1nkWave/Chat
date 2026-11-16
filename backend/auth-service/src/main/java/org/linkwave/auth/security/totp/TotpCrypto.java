package org.linkwave.auth.security.totp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hóa khóa TOTP bằng AES-256-GCM.
 * Khóa mã hóa lấy từ biến môi trường TOTP_KEY_<version> (32 byte, base64). Hỗ trợ xoay khóa:
 * khóa mới dùng TOTP_KEY_VERSION=2 và giữ TOTP_KEY_1 để giải mã dữ liệu cũ.
 */
@Component
public class TotpCrypto {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final Environment env;
    private final int currentVersion;
    private final SecureRandom random = new SecureRandom();

    public TotpCrypto(Environment env, @Value("${TOTP_KEY_VERSION:1}") int currentVersion) {
        this.env = env;
        this.currentVersion = currentVersion;
    }

    public int currentVersion() {
        return currentVersion;
    }

    public String encrypt(byte[] plain) {
        try {
            final byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(currentVersion), new GCMParameterSpec(TAG_BITS, iv));
            final byte[] cipherText = cipher.doFinal(plain);
            final byte[] out = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không mã hóa được khóa TOTP", e);
        }
    }

    public byte[] decrypt(String encoded, int version) {
        try {
            final byte[] in = Base64.getDecoder().decode(encoded);
            final ByteBuffer buffer = ByteBuffer.wrap(in);
            final byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            final byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(version), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không giải mã được khóa TOTP", e);
        }
    }

    private SecretKeySpec key(int version) {
        final String encoded = env.getProperty("TOTP_KEY_" + version);
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Thiếu biến môi trường TOTP_KEY_" + version);
        }
        final byte[] raw = Base64.getDecoder().decode(encoded.trim());
        if (raw.length != 32) {
            throw new IllegalStateException("TOTP_KEY_" + version + " phải là 32 byte, mã hóa base64");
        }
        return new SecretKeySpec(raw, "AES");
    }

}