package org.linkwave.chatservice.invitation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("group_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitation {

    @Id
    private String id;
    private String chatId;
    private Long inviterId;
    private Long inviteeId;
    private InvitationStatus status;
    private Instant createdAt;

}