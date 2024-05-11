package org.linkwave.chatservice.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MessageAuthorDto {

    private Long id;
    private String username;
    private String name;

    @JsonProperty("deleted")
    private Boolean isDeleted;

}
