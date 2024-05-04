package org.linkwave.chatservice.chat.duo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CompanionDto {

    private Long id;
    private String username;
    private String name;
    private ZonedDateTime lastSeen;

    @JsonProperty("online")
    private boolean isOnline;

}
