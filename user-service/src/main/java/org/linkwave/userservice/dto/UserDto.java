package org.linkwave.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserDto {

    private Long id;
    private String username;
    private String name;
    private ZonedDateTime lastSeen;
    private boolean isOnline;
    private String avatarPath;
    private String bio;

}