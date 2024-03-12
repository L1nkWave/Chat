package org.linkwave.chatservice.chat;

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

}
