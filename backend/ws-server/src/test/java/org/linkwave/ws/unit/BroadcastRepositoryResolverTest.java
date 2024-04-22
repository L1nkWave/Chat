package org.linkwave.ws.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.ws.repository.ChatRepository;
import org.linkwave.ws.repository.SessionRepository;
import org.linkwave.ws.websocket.routing.broadcast.BroadcastRepositoryResolverImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.linkwave.ws.unit.SessionTestUtils.generateSessionIds;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BroadcastRepositoryResolverTest {

    @Mock
    private ChatRepository<Long, String> chatRepository;

    private BroadcastRepositoryResolverImpl repositoryResolver;

    @BeforeEach
    void setUp() {
        repositoryResolver = new BroadcastRepositoryResolverImpl(
                chatRepository,
                Map.of(
                        "user:{}", SessionRepository::getUserSessions,
                        "chat:{}", ChatRepository::getSessions
                )
        );
    }

    @Test
    void Should_ResolveChatSessions_When_KeysValid() {
        final String broadcastKeyPattern = "chat:{id}";
        final String resolvedKeyPattern = "chat:xyz";
        final Set<String> expectedSessionIds = generateSessionIds(5L);

        when(chatRepository.getSessions(resolvedKeyPattern)).thenReturn(expectedSessionIds);

        final Set<String> actualSessions = repositoryResolver.resolve(broadcastKeyPattern, resolvedKeyPattern);

        verify(chatRepository, only()).getSessions(resolvedKeyPattern);
        assertThat(actualSessions).isNotNull();
        assertThat(actualSessions).isNotEmpty();
        assertThat(actualSessions).isEqualTo(expectedSessionIds);
    }

    @Test
    void Should_ResolveUserSessions_When_KeysValid() {
        final String broadcastKeyPattern = "user:{id}";
        final String resolvedKeyPattern = "user:1";
        final Set<String> expectedSessionIds = generateSessionIds(5L);

        when(chatRepository.getUserSessions(resolvedKeyPattern)).thenReturn(expectedSessionIds);

        final Set<String> actualSessions = repositoryResolver.resolve(broadcastKeyPattern, resolvedKeyPattern);

        verify(chatRepository, only()).getUserSessions(resolvedKeyPattern);
        assertThat(actualSessions).isNotNull();
        assertThat(actualSessions).isNotEmpty();
        assertThat(actualSessions).isEqualTo(expectedSessionIds);
    }

    @Test
    void Should_ThrowException_When_KeysInvalid() {
        assertThrows(NullPointerException.class, () -> repositoryResolver.resolve(null, null));
        assertThrows(NullPointerException.class, () -> repositoryResolver.resolve("chat:{id}", null));
        assertThrows(NullPointerException.class, () -> repositoryResolver.resolve(null, "chat:xyz"));
        assertThrows(IllegalArgumentException.class, () -> repositoryResolver.resolve("chat:{id}", "chat:{id}"));
    }

}
