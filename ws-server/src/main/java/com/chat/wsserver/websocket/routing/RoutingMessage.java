package com.chat.wsserver.websocket.routing;

/**
 * @param path    that message needs to reach to be handled
 * @param payload raw content of received message
 */
public record RoutingMessage(String path, String payload) {
}
