package org.linkwave.chatservice.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getUser(Long userId) {
        return userRepository.findByUserId(userId);
    }

    @Transactional
    @Override
    public User createUserIfNeed(final Long userId) {
        return getUser(userId)
                .orElseGet(() -> userRepository.save(User.builder().userId(userId).build()));
    }

    @Transactional
    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

}
