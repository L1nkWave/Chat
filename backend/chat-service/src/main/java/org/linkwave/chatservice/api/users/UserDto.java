package org.linkwave.chatservice.api.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class UserDto {

    private Long id;
    private String username;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastSeen;

    @JsonProperty("online")
    private boolean isOnline;

    private String avatarPath;
    private String bio;

}
