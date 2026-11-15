package org.linkwave.chatservice.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chatservice.api.ws.BotMessageNotification;
import org.linkwave.chatservice.api.ws.WSServiceClient;
import org.linkwave.chatservice.chat.ChatNotFoundException;
import org.linkwave.chatservice.chat.ChatService;
import org.linkwave.chatservice.common.RequestInitiator;
import org.linkwave.chatservice.message.MessageDto;
import org.linkwave.chatservice.message.MessageService;
import org.linkwave.chatservice.message.text.NewTextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.linkwave.chatservice.common.RequestUtils.userDetails;
import org.linkwave.chatservice.chat.duo.NewChatRequest;

/**
 * Trợ lý hệ thống. Chỉ phản hồi trong chat 1-1 giữa người dùng và tài khoản bot.
 * Lệnh bắt đầu bằng "/". Danh sách lệnh lọc theo vai trò của người gọi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotService {

    private static final String BOT_NAME = "SGU Bot";
    private static final Set<String> STUDENT_OR_LECTURER = Set.of("ROLE_STUDENT", "ROLE_LECTURER");

    private final ChatService chatService;
    private final MessageService messageService;
    private final WSServiceClient wsClient;

    /** ID tài khoản bot trong bảng users. 0 nghĩa là tắt bot. */
    @Value("${BOT_USER_ID:0}")
    private Long botUserId;

    /** Lệnh và vai trò được phép. Tập rỗng nghĩa là mọi tài khoản đều dùng được. */
    private record Command(String name, Set<String> roles, String description) {
    }

    private static final List<Command> COMMANDS = List.of(
            new Command("/help", Set.of(), "Xem danh sách lệnh"),
            new Command("/tkb", STUDENT_OR_LECTURER, "Thời khóa biểu"),
            new Command("/lichthi", STUDENT_OR_LECTURER, "Lịch thi"),
            new Command("/lop", STUDENT_OR_LECTURER, "Các lớp/nhóm của tôi"),
            new Command("/lichgac", Set.of("ROLE_LECTURER"), "Lịch gác thi"),
            new Command("/baocao", Set.of("ROLE_ADMIN", "ROLE_ROOT"), "Báo cáo chờ xử lý"),
            new Command("/role", Set.of("ROLE_ROOT"), "Gán/thu hồi vai trò")
    );

    /**
     * Gọi sau khi tin nhắn của người dùng đã được lưu. Không ném lỗi ra ngoài,
     * để lỗi của bot không làm hỏng tin nhắn gốc.
     */
    public void onUserMessage(@NonNull RequestInitiator initiator, String chatId, String text) {
        try {
            if (botUserId == null || botUserId == 0L) {
                return;
            }
            if (initiator.userId().equals(botUserId) || text == null || !text.startsWith("/")) {
                return;
            }
            if (!isBotDuoChat(chatId)) {
                return;
            }

            final String reply = answer(text.trim(), currentAuthorities());
            final MessageDto sent = messageService.saveTextMessage(botUserId, chatId, new NewTextMessage(reply));

            wsClient.notifyBotMessage(initiator.bearer(), new BotMessageNotification(
                    sent.getId(), chatId, botUserId, BOT_NAME, reply, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
        } catch (RuntimeException e) {
            log.warn("bot failed to answer in chat {}: {}", chatId, e.getMessage());
        }
    }

    String answer(String input, Set<String> authorities) {
        final String name = input.split("\\s+")[0].toLowerCase(Locale.ROOT);

        if (name.equals("/help")) {
            return helpText(authorities);
        }

        final Command command = COMMANDS.stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(null);
        if (command == null) {
            return "Không nhận ra lệnh \"" + name + "\". Gõ /help để xem danh sách lệnh.";
        }
        if (!allowed(command, authorities)) {
            return "Bạn không có quyền sử dụng lệnh này.";
        }
        // Dữ liệu thời khóa biểu, lịch thi... chỉ có sau khi import (giai đoạn 3-P5)
        return "Lệnh " + command.name() + " đang được phát triển. Dữ liệu sẽ có sau khi nhà trường import.";
    }

    private String helpText(Set<String> authorities) {
        final StringBuilder sb = new StringBuilder("Các lệnh bạn có thể dùng:");
        for (Command command : COMMANDS) {
            if (allowed(command, authorities)) {
                sb.append("\n").append(command.name()).append(" - ").append(command.description());
            }
        }
        return sb.toString();
    }

    private boolean allowed(Command command, Set<String> authorities) {
        return command.roles().isEmpty() || command.roles().stream().anyMatch(authorities::contains);
    }

    /** Gửi cảnh báo vào chat riêng với bot (tạo chat nếu chưa có). Không ném lỗi ra ngoài. */
    public void warnUser(@NonNull RequestInitiator initiator, String text) {
        try {
            if (botUserId == null || botUserId == 0L) {
                return;
            }
            final String chatId = findOrCreateBotChat(initiator);
            final MessageDto sent = messageService.saveTextMessage(botUserId, chatId, new NewTextMessage(text));
            wsClient.notifyBotMessage(initiator.bearer(), new BotMessageNotification(
                    sent.getId(), chatId, botUserId, BOT_NAME, text, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
        } catch (RuntimeException e) {
            log.warn("bot failed to send warning to user {}: {}", initiator.userId(), e.getMessage());
        }
    }

    private String findOrCreateBotChat(RequestInitiator initiator) {
        try {
            return chatService.findChat(initiator.userId(), botUserId).getId();
        } catch (ChatNotFoundException e) {
            return chatService.createChat(initiator, new NewChatRequest(botUserId)).getId();
        }
    }

    /** Cảnh báo người vi phạm theo quyết định của admin. Bỏ qua nếu người đó chưa có chat với bot. */
    public void warnAuthor(Long authorId, String bearer, String text) {
        try {
            if (botUserId == null || botUserId == 0L || authorId == null) {
                return;
            }
            final String chatId;
            try {
                chatId = chatService.findChat(authorId, botUserId).getId();
            } catch (ChatNotFoundException e) {
                log.warn("cannot warn author {}: no bot chat yet", authorId);
                return;
            }
            final MessageDto sent = messageService.saveTextMessage(botUserId, chatId, new NewTextMessage(text));
            wsClient.notifyBotMessage(bearer, new BotMessageNotification(
                    sent.getId(), chatId, botUserId, BOT_NAME, text, Instant.now().truncatedTo(ChronoUnit.MILLIS)));
        } catch (RuntimeException e) {
            log.warn("bot failed to warn author {}: {}", authorId, e.getMessage());
        }
    }

    /** Chat riêng với trợ lý hệ thống: không được xóa đoạn chat. */
    public boolean isBotChat(String chatId) {
        return botUserId != null && botUserId != 0L && isBotDuoChat(chatId);
    }

    private boolean isBotDuoChat(String chatId) {
        try {
            chatService.findDuoChat(chatId);
        } catch (ChatNotFoundException e) {
            return false;
        }
        return chatService.isMember(botUserId, chatId);
    }

    private Set<String> currentAuthorities() {
        return userDetails().authorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(toSet());
    }

}