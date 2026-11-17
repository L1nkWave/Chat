package org.linkwave.chatservice.api.ws;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gắn khóa nội bộ vào mọi lời gọi tới ws-server, để ws-server nhận diện đây là chat-service.
 * Các client Feign khác (user-service...) không nhận khóa này.
 */
@Component
public class WsInternalKeyInterceptor implements RequestInterceptor {

    private static final String WS_SERVER = "ws-server";
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final String internalKey;

    public WsInternalKeyInterceptor(@Value("${internal.service-key}") String internalKey) {
        this.internalKey = internalKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        final Target<?> target = template.feignTarget();
        if (target != null && WS_SERVER.equals(target.name())) {
            template.header(INTERNAL_KEY_HEADER, internalKey);
        }
    }
}