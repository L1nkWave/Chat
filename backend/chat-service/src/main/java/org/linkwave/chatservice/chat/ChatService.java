package org.linkwave.chatservice.chat;

import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.duo.NewChatRequest;
import org.linkwave.chatservice.chat.group.*;
import org.linkwave.chatservice.common.PrivacyViolationException;
import org.linkwave.chatservice.common.RequestInitiator;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatService {

    /**
     * Create duo chat between request initiator and specified recipient
     *
     * @param initiator   identified user that made the request
     * @param chatRequest recipient definition
     * @return dto representation of the created chat
     */
    ChatDto createChat(@NonNull RequestInitiator initiator, @NonNull NewChatRequest chatRequest);

    /**
     * Create group chat with initiator as admin of the chat.
     *
     * @param initiator   identified user that made the request
     * @param chatRequest necessary properties for new group chat
     * @return dto representation of the created group chat
     */
    GroupChatDto createGroupChat(@NonNull RequestInitiator initiator, @NonNull NewGroupChatRequest chatRequest);

    /**
     * Inter-service method to find a duo / group chat by specified ID.
     *
     * @param id chat's identification
     * @return entity (DAO) that represents the duo / group chat
     * @throws ChatNotFoundException if chat was not found with passed ID
     */
    Chat findChat(String id) throws ChatNotFoundException;

    /**
     * Returns duo chat for both users. The users' order does not matter.
     *
     * @param userId  id of the 1st user
     * @param userId2 id of the 2nd user
     * @return chat object
     * @throws ChatNotFoundException when chat for both users not found
     */
    Chat findChat(Long userId, Long userId2) throws ChatNotFoundException;

    /**
     * Returns duo chat for specified identification.
     *
     * @param id chat's identification
     * @return entity (DAO) that represents the duo chat
     * @throws ChatNotFoundException if chat was not found with passed ID
     */
    Chat findDuoChat(String id) throws ChatNotFoundException;

    /**
     * Inter-service method to find a group chat by specified ID.
     *
     * @param id chat's identification
     * @return entity (DAO) that represents the group chat
     * @throws ChatNotFoundException if chat was not found with passed ID
     */
    GroupChat findGroupChat(String id) throws ChatNotFoundException;

    /**
     * Returns mapped duo / group chat by specified ID.
     *
     * @param id        chat's identification
     * @param initiator author of request
     * @return mapped chat dto
     * @throws ChatNotFoundException     if chat was not found with passed ID
     * @throws PrivacyViolationException if initiator is not a chat member
     */
    ChatDto getGenericChat(String id, RequestInitiator initiator) throws ChatNotFoundException, PrivacyViolationException;

    /**
     * Retrieve a portion of chats for initiator bounded with {@code offset} and {@code limit} parameters.
     *
     * @param initiator identified user that made the request
     * @param offset    how many chats to skip
     * @param limit     how many chats to retrieve
     * @return pair which contains total count of chats initiator involved in as first value,
     * and selected list of chats as the second
     */
    Pair<Long, List<ChatDto>> getUserChats(@NonNull RequestInitiator initiator, int offset, int limit);

    /**
     * Returns ids for all chats user is involved in.
     *
     * @param userId any user ID
     * @return list of chats ids
     */
    List<String> getUserChats(Long userId);

    /**
     * Inter-service method to update attributes of the passed chat. Works for both {@link Chat} and {@link GroupChat}.
     * This can also be used for saving.
     *
     * @param chat the chat that is necessary to update
     */
    void updateChat(@NonNull Chat chat);

    /**
     * Returns members for each chat that user (userId) is a member of. Every chat from the {@code chatIds} list
     * is checked to determine if user is a member too. When it is, the members of that chat is retrieved, otherwise
     * the chat is gonna to be skipped.
     *
     * @param userId  id of the user
     * @param chatIds chats to return members for
     * @return map where key is chat id, and value is list of members of this chat
     */
    Map<String, List<ChatMember>> getChatsMembers(Long userId, List<String> chatIds);

    /**
     * Used to get to know if user is a member of specific chat.
     * Works for both {@link Chat} and {@link GroupChat}.
     *
     * @param userId any user ID
     * @param chatId any chat ID
     * @return true if passed user is involved in specified chat, otherwise - false.
     */
    boolean isMember(Long userId, String chatId);

    /**
     * An extended version for {@link ChatService#isMember(Long, String)}.
     *
     * @param userId any user ID
     * @param chat   any chat entity,
     * @return true if passed user is involved in specified chat, otherwise - false.
     */
    boolean isMember(Long userId, @NonNull Chat chat);

    /**
     * An extended version for others {@link ChatService#isMember} methods.
     * Provides the opportunity to get chat member explicitly. Returned object
     * can be used to perform additional checks for membership roles, etc.
     *
     * @param userId user identification
     * @param chat   to find member in
     * @return optional object, that may contain value if member was found,
     * otherwise - it is empty
     */
    Optional<ChatMember> findChatMember(Long userId, @NonNull Chat chat);

    boolean isAdmin(Long memberId, @NonNull Chat chat);

    /**
     * Checks whether member has specified role in the chat.
     *
     * @param chat     duo / group chat
     * @param memberId member that is needed to check role for
     * @param role     role is needed to check
     * @throws ChatMemberPermissionsDenied when member not found or does not have the specified role
     */
    void checkMemberRole(@NonNull Chat chat, Long memberId, ChatRole role) throws ChatMemberPermissionsDenied;

    ChatMemberDto addGroupChatMember(String chatId, @NonNull RequestInitiator initiator);

    ChatMemberDto addGroupChatMember(String chatId, @NonNull RequestInitiator initiator, Long userId);

    void removeGroupChatMember(Long userId, String chatId);

    ChatMemberDto removeGroupChatMember(String chatId, @NonNull RequestInitiator initiator, Long memberId);

    void changeMemberRole(String chatId, Long initiatorId, Long memberId, ChatRole newRole);

    void updateGroupChat(Long initiatorId, String chatId, @NonNull UpdateGroupChat updateGroupChat)
            throws ChatMemberPermissionsDenied;

    void removeGroupChat(Long initiatorId, String chatId) throws ChatMemberPermissionsDenied;

    /**
     * Returns group chat details if initiator has access to it.
     *
     * @param initiator identified user that made the request
     * @param chatId    any chat ID
     * @return group chat details dto representation
     * @throws PrivacyViolationException if initiator is not a member of the specified chat
     */
    GroupChatDetailsDto getGroupChatDetails(@NonNull RequestInitiator initiator, String chatId)
            throws PrivacyViolationException;

    /**
     * Used to set avatar for specified group chat.
     *
     * @param chatId group chat identification
     * @param avatar image content
     */
    void changeGroupChatAvatar(String chatId, @NonNull MultipartFile avatar);

    /**
     * @param chat non-null group chat object
     * @return true if avatar is set, otherwise false
     */
    boolean isAvatarSet(@NonNull GroupChat chat);

    /**
     * Retrieve a group chat avatar as array of bytes.
     *
     * @param chatId group chat identification
     * @return image that consists of bytes
     */
    byte[] getGroupChatAvatar(String chatId);

    /**
     * Remove a group chat avatar if it exists.
     *
     * @param chatId group chat identification for remove avatar
     */
    void deleteGroupChatAvatar(String chatId);
}
