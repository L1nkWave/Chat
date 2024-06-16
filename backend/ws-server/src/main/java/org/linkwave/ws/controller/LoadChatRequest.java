package org.linkwave.ws.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoadChatRequest {

    @NotBlank(message = "must be present")
    private String chatId;

    @NotNull(message = "must be present")
    @Min(value = 1, message = "must be bigger than 0")
    @Max(value = Long.MAX_VALUE, message = "max limit is exceeded")
    private Long recipientId;

}
