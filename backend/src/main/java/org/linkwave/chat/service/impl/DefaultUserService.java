package org.linkwave.chat.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chat.dto.*;
import org.linkwave.chat.entity.RoleEntity;
import org.linkwave.chat.entity.UserEntity;
import org.linkwave.chat.repository.RoleRepository;
import org.linkwave.chat.repository.UserRepository;
import org.linkwave.chat.security.JwtProvider;
import org.linkwave.chat.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.linkwave.chat.entity.RoleEntity.Roles.USER;
import static org.linkwave.chat.security.Token.ACCESS;
import static org.linkwave.chat.security.Token.REFRESH;

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
    private final JwtProvider jwtProvider;


    @Transactional
    @Override
    public AuthDto register(UserRegisterRequest registerRequest) {
        log.info("-> register()");

        Optional<UserEntity> user = userRepository.findByUsername(registerRequest.getUsername());
        if (user.isPresent()) {
            throw new BadCredentialsException("username is already taken");
        }

        RoleEntity defaultRole = roleRepository.findByName(USER.getName())
                .orElseThrow(() -> new IllegalStateException("role_user is not found"));

        UserEntity newUser = UserEntity.builder()
                .name(registerRequest.getName())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .lastSeen(Instant.EPOCH.atZone(ZoneId.systemDefault()))
                .avatarPath(String.format("%s%s%s", avatarPath, File.separator, defaultAvatarName))
                .roles(List.of(defaultRole))
                .build();

        newUser = userRepository.save(newUser);

        AuthDto authDto = generateTokens(newUser);
        newUser.setRefreshToken(authDto.getJwtRefresh());
        return authDto;
    }

    @Transactional
    @Override
    public AuthDto login(UserLoginRequest loginRequest) {
        log.info("-> login()");

        UserEntity user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));

        boolean arePasswordsSame = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
        if (!arePasswordsSame) {
            throw new BadCredentialsException("bad credentials");
        }

        AuthDto authDto = generateTokens(user);
        user.setRefreshToken(authDto.getJwtRefresh());
        return authDto;
    }

    @Override
    public AuthDto refresh(UserRefreshRequest refreshRequest) {
        log.info("-> refresh()");

        UserEntity user = userRepository.findByRefreshToken(refreshRequest.getJwtRefresh())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));

        AuthDto authDto = generateTokens(user);
        user.setRefreshToken(authDto.getJwtRefresh());
        return authDto;
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

    private AuthDto generateTokens(UserEntity user) {
        String jwtAccess = jwtProvider.generateToken(user, ACCESS);
        String jwtRefresh = jwtProvider.generateToken(user, REFRESH);
        return new AuthDto(jwtAccess, jwtRefresh);
    }

}