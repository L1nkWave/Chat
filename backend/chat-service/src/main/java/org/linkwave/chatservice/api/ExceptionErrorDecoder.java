package org.linkwave.chatservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExceptionErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public RuntimeException decode(String method, @NonNull Response response) {
        try {
            final var errorAttributes = objectMapper.readValue(response.body().asInputStream(), Map.class);
            final HttpStatus httpStatus = HttpStatus.valueOf(response.status());
            if (httpStatus.is4xxClientError()) {
                return new ApiResponseClientErrorException(errorAttributes.get("message").toString());
            }
            return new ServiceErrorException();
        } catch (Exception e) {
            return new ServiceErrorException("Internal server error");
        }
    }

}
