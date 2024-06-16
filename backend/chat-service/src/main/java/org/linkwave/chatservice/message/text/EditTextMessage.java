package org.linkwave.chatservice.message.text;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class EditTextMessage {

    @NotNull(message = "must be present")
    @Length(
            min = 1, max = 512,
            message = "length must be in range [1, 512]"
    )
    private String text;

}
