package com.chat.wsserver.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Component
public class WebSocketSessionConfigurerImpl implements WebSocketSessionConfigurer {

    @Value("${ws.session.concurrent.send-time-limit}")
    private int sendTimeLimit;

    @Value("${ws.session.concurrent.buffer-size-limit}")
    private int bufferSizeLimit;

    @Override
    public WebSocketSession configure(@NonNull WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, sendTimeLimit, bufferSizeLimit);
    }

}
