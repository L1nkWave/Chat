package org.linkwave.auth.security.totp;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** TOTP theo RFC 6238 (HMAC-SHA1, 6 chữ số, bước 30 giây). Tự cài đặt, không phụ thuộc thư viện ngoài. */
public final class TotpAlgorithm {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int STEP_SECONDS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpAlgorithm() {
    }

    public static String newSecretBase32() {
        final byte[] raw = new byte[20];
        RANDOM.nextBytes(raw);
        return base32Encode(raw);
    }

    public static long currentStep(long epochSeconds) {
        return epochSeconds / STEP_SECONDS;
    }

    /**
     * Tìm bước khớp với mã trong cửa sổ ±1 bước. Bỏ qua bước đã dùng (lastUsedStep).
     * @return bước khớp, hoặc -1 nếu không khớp
     */
    public static long matchStep(byte[] key, String code, long nowStep, long lastUsedStep) {
        final byte[] given = code.getBytes(StandardCharsets.UTF_8);
        for (long step = nowStep - 1; step <= nowStep + 1; step++) {
            if (step <= lastUsedStep) {
                continue;
            }
            final byte[] expected = code(key, step).getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(expected, given)) {
                return step;
            }
        }
        return -1;
    }

    static String code(byte[] key, long step) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            final byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            final int offset = hash[hash.length - 1] & 0x0F;
            final int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA1 không khả dụng", e);
        }
    }

    public static String base32Encode(byte[] data) {
        final StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = ((buffer << 8) | (b & 0xFF)) & 0xFFFF;
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    public static byte[] base32Decode(String encoded) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : encoded.toUpperCase().toCharArray()) {
            if (c == '=') {
                break;
            }
            final int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Ký tự base32 không hợp lệ");
            }
            buffer = ((buffer << 5) | value) & 0xFFFF;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

}