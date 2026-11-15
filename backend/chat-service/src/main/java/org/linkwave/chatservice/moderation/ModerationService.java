package org.linkwave.chatservice.moderation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ModerationService {

    public static final String INVALID_TEXT = "Tin nhắn không hợp lệ";
    public static final String WARNING_TEXT =
            "Cảnh báo: tin nhắn của bạn chứa từ ngữ không phù hợp nên đã bị ẩn. Vui lòng giữ thái độ lịch sự khi trao đổi.";

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final int MAX_QUOTE = 200;

    private final WordFilter filter;

    public ModerationService() {
        this.filter = new WordFilter(loadWords());
    }

    public Optional<String> findViolation(String text) {
        return filter.findViolation(text);
    }

    /** Nội dung cảnh báo gửi cho người vi phạm: ai, lúc nào, và tin nhắn nào. */
    public String warningFor(String username, String text, Instant at) {
        final String quoted = text.length() > MAX_QUOTE ? text.substring(0, MAX_QUOTE) + "…" : text;
        return "Cảnh báo: tin nhắn của bạn (" + username + ") lúc " + TIME.format(at.atZone(ZONE))
                + " có từ ngữ không phù hợp nên đã bị ẩn. Nội dung: \"" + quoted + "\"";
    }

    /** Ghi nhận vi phạm để đối soát. Không ghi nội dung tin nhắn. */
    public void recordViolation(Long userId, String target) {
        log.info("moderation: auto-blocked message, user={}, target={}", userId, target);
    }

    private static List<String> loadWords() {
        try (var in = new ClassPathResource("moderation/blocked-words.txt").getInputStream()) {
            final String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.asList(content.split("\\R"));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read moderation/blocked-words.txt", e);
        }
    }

}