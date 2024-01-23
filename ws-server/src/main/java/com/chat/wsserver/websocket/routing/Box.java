package com.chat.wsserver.websocket.routing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Box<T> {

    private T value;
    private Object errorValue;

    public boolean hasError() {
        return errorValue != null;
    }

    @NonNull
    public static <T> Box<T> ok(T value) {
        return new Box<>(value, null);
    }

    @NonNull
    public static <T> Box<T> error(Object errorValue) {
        return new Box<>(null, errorValue);
    }

}
