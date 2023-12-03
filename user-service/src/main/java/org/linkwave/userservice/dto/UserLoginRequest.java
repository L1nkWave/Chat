package org.linkwave.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserLoginRequest {

    @NotNull(message = "can't be null")
    @Size(min = 3, max = 32, message = "length should be from 3 to 32 characters")
    private String username;

    @NotNull(message = "can't be null")
    @Size(min = 3, max = 64, message = "length should be from 3 to 64 characters")
    private String password;

}