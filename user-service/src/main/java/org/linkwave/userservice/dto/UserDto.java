package org.linkwave.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserDto {

    private Long id;
    private String name;
    private String username;
    private ZonedDateTime registeredAt;
    private boolean theme;
    private List<String> roles;

}