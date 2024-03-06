package org.linkwave.chatservice.chat.duo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class NewChatRequest {

    @NotNull(message = "must be present")
    @Min(value = 1, message = "must be bigger than 0")
    @Max(value = Long.MAX_VALUE, message = "max limit is exceeded")
    private Long recipient;

}
