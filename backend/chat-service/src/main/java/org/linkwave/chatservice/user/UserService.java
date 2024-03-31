package org.linkwave.chatservice.user;

import java.util.Optional;

public interface UserService {
    Optional<User> getUser(Long userId);

    void createUserIfNeed(Long userId);

    User save(User user);
}
