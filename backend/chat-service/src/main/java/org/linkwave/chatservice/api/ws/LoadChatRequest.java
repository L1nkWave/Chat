package org.linkwave.chatservice.api.ws;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class LoadChatRequest {

    private String chatId;
    private Long recipientId;

}
