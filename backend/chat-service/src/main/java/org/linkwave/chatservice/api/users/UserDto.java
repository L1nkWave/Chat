package org.linkwave.chatservice.api.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class UserDto {

    private Long id;
    private String username;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastSeen;

    @JsonProperty("online")
    private boolean isOnline;

    private String avatarPath;

    @JsonProperty("deleted")
    private boolean isDeleted;

    @JsonProperty("blocked")
    private boolean isBlocked;

    private String bio;

}