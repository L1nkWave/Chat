package org.linkwave.ws.api.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@Builder
public class GroupChatDto {

    private String id;
    private Instant createdAt;
    private String name;
    private String avatarPath;

}
