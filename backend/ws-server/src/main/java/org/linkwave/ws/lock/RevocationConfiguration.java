package org.linkwave.ws.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * Nghe kênh thu hồi phiên từ auth-service. Container này tách với container của broadcast (RedisConfiguration).
 */
@Slf4j
@Configuration
public class RevocationConfiguration {

    @Bean
    public RedisMessageListenerContainer revocationListenerContainer(RedisConnectionFactory connectionFactory,
                                                                     AccountLockService lockService) {
        final var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            final String body = new String(message.getBody(), StandardCharsets.UTF_8).trim();
            try {
                final int closed = lockService.closeSessions(Long.parseLong(body));
                log.info("-> revoke: user {} đã đổi mật khẩu hoặc đăng xuất mọi thiết bị, đóng {} phiên WebSocket", body, closed);
            } catch (NumberFormatException e) {
                log.warn("-> revoke: userId không hợp lệ: {}", body);
            }
        }, new ChannelTopic(SessionRevocation.CHANNEL));
        return container;
    }
}