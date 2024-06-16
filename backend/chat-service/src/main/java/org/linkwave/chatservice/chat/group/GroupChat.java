package org.linkwave.chatservice.chat.group;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.linkwave.chatservice.chat.ChatMember;
import org.linkwave.chatservice.chat.ChatRole;
import org.linkwave.chatservice.chat.duo.Chat;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.NonNull;

import java.time.Instant;

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

    public ChatMember addMember(@NonNull Long userId) {
        final var newMember = new ChatMember(userId, ChatRole.MEMBER, Instant.now());
        getMembers().add(newMember);
        membersCount++;
        return newMember;
    }

    public void removeMember(@NonNull Long userId) {
        final boolean isRemoved = getMembers().removeIf(member -> member.getId().equals(userId));
        if (isRemoved) {
            membersCount--;
        }
    }

}
