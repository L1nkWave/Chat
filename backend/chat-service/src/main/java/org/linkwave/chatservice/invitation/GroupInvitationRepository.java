package org.linkwave.chatservice.invitation;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GroupInvitationRepository extends MongoRepository<GroupInvitation, String> {

    List<GroupInvitation> findByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    boolean existsByChatIdAndInviteeIdAndStatus(String chatId, Long inviteeId, InvitationStatus status);

}