package org.linkwave.chatservice.chat.group;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class NewGroupChatRequest {

    @NotNull(message = "must be present")
    @Length(min = 1, max = 32, message = "length should be in range [1, 32]")
    private String name;

    @Length(max = 256, message = "length limit of 128 is exceeded")
    private String description;

    @NotNull(message = "must be present")
    private Boolean isPrivate;

}
