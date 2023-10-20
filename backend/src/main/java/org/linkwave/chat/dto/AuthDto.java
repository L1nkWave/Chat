package org.linkwave.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AuthDto {
    private String jwtAccess;
    private String jwtRefresh;
}