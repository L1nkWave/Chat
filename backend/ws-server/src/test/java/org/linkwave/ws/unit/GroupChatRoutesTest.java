package org.linkwave.ws.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.shared.utils.Bearers;
import org.linkwave.ws.api.chat.ApiErrorException;
import org.linkwave.ws.api.chat.ChatServiceClient;
import org.linkwave.ws.api.chat.GroupChatDto;
import org.linkwave.ws.websocket.dto.Action;
import org.linkwave.ws.websocket.dto.ChatMessage;
import org.linkwave.ws.websocket.dto.ErrorMessage;
import org.linkwave.ws.websocket.dto.NewGroupChat;
import org.linkwave.ws.websocket.jwt.UserPrincipal;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.websocket.route.GroupChatRoutes;
import org.linkwave.ws.websocket.routing.Box;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.linkwave.ws.unit.SessionTestUtils.createSession;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupChatRoutesTest {

    @Mock
    private ChatServiceClient chatServiceClient;

    @Mock
    private ChatRepository<Long, String> chatRepository;

    @InjectMocks
    private GroupChatRoutes routes;

    @Test
    void Should_CreateGroupChat_When_ApiResponseSuccess() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var rawToken = sessionPair.getFirst().rawAccessToken();
        final var body = new NewGroupChat("Chat name", "description", true);

        final GroupChatDto apiResponse = GroupChatDto.builder()
                .name(body.getName())
                .createdAt(Instant.now())
                .id(UUID.randomUUID().toString())
                .build();

        when(chatServiceClient.createGroupChat(Bearers.append(rawToken), body)).thenReturn(apiResponse);

        final Box<GroupChatDto> result = routes.createChat(sessionPair.getFirst(), body, "");

        assertThat(result.hasError()).isFalse();
        assertThat(result.getValue()).isEqualTo(apiResponse);
    }

    @Test
    void Should_FailGroupChatCreation_When_ApiResponseError() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var rawToken = sessionPair.getFirst().rawAccessToken();

        final var body = new NewGroupChat(null, "description", true);

        final String expectedError = "Name must be present";
        when(chatServiceClient.createGroupChat(Bearers.append(rawToken), body))
                .thenThrow(new ApiErrorException(expectedError));

        final Box<GroupChatDto> result = routes.createChat(sessionPair.getFirst(), body, "");

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorValue()).isInstanceOf(ErrorMessage.class);
        assertThat(((ErrorMessage) result.getErrorValue()).getError()).isEqualTo(expectedError);
    }

    @Test
    void Should_JoinInChat_When_IsNotMemberAndApiResponseSuccess() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var principal = sessionPair.getFirst();
        final Long userId = principal.token().userId();

        final String chatId = UUID.randomUUID().toString();
        when(chatRepository.isMember(chatId, userId)).thenReturn(FALSE);

        final Box<ChatMessage> result = routes.join(chatId, principal, "");
        final ChatMessage message = result.getValue();

        verify(chatRepository, times(1)).isMember(chatId, userId);
        assertThat(result.hasError()).isFalse();
        assertThat(message.getChatId()).isEqualTo(chatId);
        assertThat(message.getAction()).isEqualTo(Action.JOIN);
    }

    @Test
    void Should_ReturnErrorMessage_When_JoinInChatBeingItsMemberAlready() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var principal = sessionPair.getFirst();
        final Long userId = principal.token().userId();

        final String chatId = UUID.randomUUID().toString();
        when(chatRepository.isMember(chatId, userId)).thenReturn(TRUE);
        final String expectedError = "You are already a member of chat";

        final Box<ChatMessage> result = routes.join(chatId, principal, "");

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorValue()).isNotNull();
        assertThat(result.getErrorValue()).isInstanceOf(ErrorMessage.class);
        assertThat(((ErrorMessage) result.getErrorValue()).getError()).isEqualTo(expectedError);
    }

    @Test
    void Should_ReturnErrorMessage_When_ApiResponseErrorOnJoinInChat() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var principal = sessionPair.getFirst();
        final Long userId = principal.token().userId();

        final String chatId = UUID.randomUUID().toString();
        when(chatRepository.isMember(chatId, userId)).thenReturn(FALSE);

        final String expectedError = "Chat is inaccessible at the moment";
        doThrow(new ApiErrorException(expectedError))
                .when(chatServiceClient)
                .joinGroupChat(Bearers.append(principal.rawAccessToken()), chatId);

        final Box<ChatMessage> result = routes.join(chatId, principal, "");

        verify(chatRepository, only()).isMember(chatId, userId);
        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorValue()).isNotNull();
        assertThat(result.getErrorValue()).isInstanceOf(ErrorMessage.class);
        assertThat(((ErrorMessage) result.getErrorValue()).getError()).isEqualTo(expectedError);
    }

    @Test
    void Should_LeaveChat_When_IsMemberAndApiResponseSuccess() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var principal = sessionPair.getFirst();
        final Long userId = principal.token().userId();

        final String chatId = UUID.randomUUID().toString();
        when(chatRepository.isMember(chatId, userId)).thenReturn(TRUE);

        final Box<ChatMessage> result = routes.leaveChat(chatId, principal, "");

        assertThat(result.hasError()).isFalse();
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().getChatId()).isEqualTo(chatId);
    }

    @Test
    void Should_ReturnErrorMessage_When_IsNotMember() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var principal = sessionPair.getFirst();
        final Long userId = principal.token().userId();

        final String chatId = UUID.randomUUID().toString();
        when(chatRepository.isMember(chatId, userId)).thenReturn(FALSE);

        final String expectedError = "You are not member of chat";

        final Box<ChatMessage> result = routes.leaveChat(chatId, principal, "");

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorValue()).isNotNull();
        assertThat(result.getErrorValue()).isInstanceOf(ErrorMessage.class);
        assertThat(((ErrorMessage)result.getErrorValue()).getError()).isEqualTo(expectedError);
    }

    @Test
    void Should_ReturnErrorMessage_When_ApiResponseError() {
        final Pair<UserPrincipal, WebSocketSession> sessionPair = createSession(false);
        final var principal = sessionPair.getFirst();
        final Long userId = principal.token().userId();

        final String chatId = UUID.randomUUID().toString();
        when(chatRepository.isMember(chatId, userId)).thenReturn(TRUE);

        final String expectedError = "Bad request";
        doThrow(new ApiErrorException(expectedError))
                .when(chatServiceClient)
                .leaveGroupChat(Bearers.append(principal.rawAccessToken()), chatId);

        final Box<ChatMessage> result = routes.leaveChat(chatId, principal, "");

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorValue()).isNotNull();
        assertThat(((ErrorMessage)result.getErrorValue()).getError()).isEqualTo(expectedError);
    }

}
