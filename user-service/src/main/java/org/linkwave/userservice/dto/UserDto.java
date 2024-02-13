package org.linkwave.userservice.dto;

import lombok.*;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@Builder
public class UserDto {

    private Long id;
    private String username;
    private String name;
    private ZonedDateTime lastSeen;
    private boolean isOnline;
    private String avatarPath;
    private String bio;

}