package org.linkwave.ws.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.linkwave.shared.dto.ApiError;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public Exception decode(String s, @NonNull Response response) {
        final var apiError = objectMapper.readValue(response.body().asInputStream(), ApiError.class);
        return new ApiErrorException(apiError.message());
    }

}
