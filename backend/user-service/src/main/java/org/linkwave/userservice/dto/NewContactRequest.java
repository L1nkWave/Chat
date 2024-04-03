package org.linkwave.userservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class NewContactRequest {

    @NotNull(message = "must be present")
    @Min(value = 1, message = "value must be bigger than 0")
    private Long userId;

    @NotNull(message = "must be present")
    @Length(min = 3, max = 64, message = "length must be from 3 to 64")
    private String alias;

}
