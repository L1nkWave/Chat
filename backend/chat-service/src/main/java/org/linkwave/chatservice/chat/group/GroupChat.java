package org.linkwave.chatservice.chat.group;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.duo.Chat;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("chats")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class GroupChat extends Chat {

    private String name;
    private String description;
    private String avatarPath;
    private int membersCount;

    @Builder.Default
    private int membersLimit = 20;

    @Builder.Default
    private boolean isPrivate = true;

}
