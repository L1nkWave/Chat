package org.linkwave.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.userservice.dto.*;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.RoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.linkwave.userservice.entity.RoleEntity.Roles.USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

    @Value("${files.avatar.path}")
    private String avatarPath;

    @Value("${files.avatar.default}")
    private String defaultAvatarName;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void register(@NonNull UserRegisterRequest registerRequest) {
        log.debug("-> register()");

        Optional<UserEntity> user = userRepository.findByUsername(registerRequest.getUsername());
        if (user.isPresent()) {
            throw new BadCredentialsException("username is already taken");
        }

        RoleEntity defaultRole = roleRepository.findByName(USER.getName())
                .orElseThrow(() -> new IllegalStateException("role_user is not found"));

        var newUser = UserEntity.builder()
                .name(registerRequest.getName())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .lastSeen(Instant.EPOCH.atZone(ZoneId.systemDefault()))
                .avatarPath(String.format("%s%s%s", avatarPath, File.separator, defaultAvatarName))
                .roles(List.of(defaultRole))
                .build();

        userRepository.save(newUser);
    }

    @Override
    public UserDto getPersonalInfo(Long userId) {
        log.info("-> getPersonalInfo()");

        UserEntity user = userRepository.findUserWithRoles(userId)
                .orElseThrow(() -> new IllegalStateException("user not found"));

        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .toList();

        return new UserDto(
                user.getId(), user.getName(), user.getUsername(),
                user.getCreatedAt(), user.isTheme(), roles
        );
    }

}