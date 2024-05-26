package org.linkwave.chatservice.chat.group;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record UpdateGroupChat(
        @NotNull @Length(min = 1, max = 32)
        String name,

        @Length(max = 256)
        String description,

        @NotNull @Min(0) @Max(1000)
        Integer membersLimit,

        @NotNull
        Boolean isPrivate
) {
}
