package org.linkwave.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserRefreshRequest {

    @NotNull(message = "can't be null")
    @NotBlank(message = "can't be blank")
    private String jwtRefresh;

}