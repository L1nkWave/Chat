package org.linkwave.userservice.academic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Mã hóa CCCD trước khi lưu DB.
 * - Giá trị lưu: AES-256-GCM, IV ngẫu nhiên 12 byte cho mỗi lần mã hóa, định dạng "v1:&lt;iv&gt;:&lt;ciphertext+tag&gt;".
 * - Chỉ mục chống trùng và tra cứu: HMAC-SHA256 với khóa riêng, không thể đảo ngược nếu không có khóa.
 * Khóa lấy từ biến môi trường CCCD_ENCRYPTION_KEY và CCCD_INDEX_KEY (mỗi khóa 32 byte, mã base64).
 * Thiếu hoặc sai khóa thì ứng dụng không khởi động.
 */
@Component
public class CccdCipher {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final Pattern TWELVE_DIGITS = Pattern.compile("\\d{12}");

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec indexKey;
    private final SecureRandom random = new SecureRandom();

    public CccdCipher(@Value("${app.cccd.key}") String encryptionKeyBase64,
                      @Value("${app.cccd.index-key}") String indexKeyBase64) {
        this.encryptionKey = new SecretKeySpec(requireKey(encryptionKeyBase64, "CCCD_ENCRYPTION_KEY"), "AES");
        this.indexKey = new SecretKeySpec(requireKey(indexKeyBase64, "CCCD_INDEX_KEY"), "HmacSHA256");
    }

    /** CCCD hợp lệ là đúng 12 chữ số */
    public static boolean isValid(String cccd) {
        return cccd != null && TWELVE_DIGITS.matcher(cccd).matches();
    }

    /** Hiển thị: giữ 4 số đầu, che phần còn lại */
    public static String mask(String cccd) {
        if (cccd == null || cccd.length() <= 4) {
            return "****";
        }
        return cccd.substring(0, 4) + "*".repeat(cccd.length() - 4);
    }

    public String encrypt(String plain) {
        try {
            final byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            final byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không mã hóa được CCCD", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalStateException("Dữ liệu CCCD không đúng định dạng");
        }
        final String[] parts = stored.substring(PREFIX.length()).split(":", -1);
        if (parts.length != 2) {
            throw new IllegalStateException("Dữ liệu CCCD không đúng định dạng");
        }
        try {
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey,
                    new GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không giải mã được CCCD (sai khóa hoặc dữ liệu bị sửa)", e);
        }
    }

    public String index(String plain) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(indexKey);
            return HexFormat.of().formatHex(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chỉ mục CCCD", e);
        }
    }

    private static byte[] requireKey(String base64, String envName) {
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64 == null ? "" : base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(envName + " phải là chuỗi base64");
        }
        if (bytes.length != 32) {
            throw new IllegalStateException(envName + " phải giải mã ra đúng 32 byte (tạo bằng: openssl rand -base64 32)");
        }
        return bytes;
    }
}