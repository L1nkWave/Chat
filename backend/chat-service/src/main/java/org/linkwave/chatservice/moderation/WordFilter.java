package org.linkwave.chatservice.moderation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

/**
 * Kiểm tra văn bản có chứa từ cấm hay không.
 * Chuẩn hóa: bỏ dấu tiếng Việt, chữ thường, đổi chữ số/ký hiệu giống chữ (leet).
 * Hai cách so khớp:
 * - theo từ: so khớp đúng từ (hoặc cụm từ) có ranh giới khoảng trắng;
 * - theo chuỗi chữ cái: bắt được kiểu viết cách/chấm giữa các chữ (f.u.c.k). Chỉ dùng cho từ từ 4 chữ cái trở lên
 *   để giảm báo nhầm (ví dụ "class" chứa "ass" nhưng "ass" quá ngắn nên không áp dụng).
 */
public class WordFilter {

    private static final int MIN_COMPACT_LENGTH = 4;

    private final List<String> spacedWords = new ArrayList<>();
    private final List<String> compactWords = new ArrayList<>();

    public WordFilter(List<String> rawWords) {
        for (String raw : rawWords) {
            final String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final String normalized = normalize(line);
            final String spaced = words(normalized);
            final String compact = normalized.replaceAll("[^a-z]", "");
            if (!spaced.isEmpty()) {
                spacedWords.add(spaced);
            }
            if (compact.length() >= MIN_COMPACT_LENGTH) {
                compactWords.add(compact);
            }
        }
    }

    public Optional<String> findViolation(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        final String normalized = normalize(text);
        final String spaced = " " + words(normalized) + " ";

        for (String word : spacedWords) {
            if (spaced.contains(" " + word + " ")) {
                return Optional.of(word);
            }
        }

        final String compact = normalized.replaceAll("[^a-z]", "");
        for (String word : compactWords) {
            if (compact.contains(word)) {
                return Optional.of(word);
            }
        }
        return Optional.empty();
    }

    static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't')
                .replace('@', 'a')
                .replace('$', 's');
    }

    /** Các cụm chữ cái, nối bằng một khoảng trắng. */
    static String words(String normalized) {
        return Arrays.stream(normalized.split("[^a-z]+"))
                .filter(token -> !token.isEmpty())
                .collect(joining(" "));
    }

}