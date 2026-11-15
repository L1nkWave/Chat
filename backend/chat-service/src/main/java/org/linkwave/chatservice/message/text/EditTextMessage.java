package org.linkwave.chatservice.message.text;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
public class EditTextMessage {

    @NotNull(message = "must be present")
    @Length(
            min = 1, max = 512,
            message = "length must be in range [1, 512]"
    )
    private String text;

    public EditTextMessage(String text) {
        this.text = text;
    }

}