package org.linkwave.chatservice.user;

import java.util.Optional;

public interface UserService {
    Optional<User> getUser(Long userId);

    User createUserIfNeed(Long userId);

    User save(User user);
}
