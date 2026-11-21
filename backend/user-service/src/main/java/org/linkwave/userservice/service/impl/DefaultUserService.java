package org.linkwave.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.shared.storage.FileStorageService;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.RoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.text.Normalizer;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.lang.String.valueOf;
import static org.linkwave.userservice.entity.RoleEntity.Roles.USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

    public static final Path USER_AVATAR_PATH = Path.of("api", "users");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public UserEntity findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User[%d] not found".formatted(userId)));
    }

    @Transactional
    @Override
    public void register(@NonNull UserRegisterRequest registerRequest) {
        log.debug("-> register()");

        final Optional<UserEntity> user = userRepository.findByUsername(registerRequest.getUsername());
        if (user.isPresent()) {
            throw new BadCredentialsException("username is already taken");
        }

        final RoleEntity defaultRole = roleRepository.findByName(USER.getValue())
                .orElseThrow(() -> new IllegalStateException("role_user is not found"));

        final var newUser = UserEntity.builder()
                .name(registerRequest.getName())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(List.of(defaultRole))
                .build();

        userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUser(Long id) {
        log.debug("-> getUser()");
        return modelMapper.map(findById(id), UserDto.class);
    }

    @Transactional
    @Override
    public void setUserStatus(Long id, @NonNull Boolean isOnline) {
        final UserEntity user = findById(id);
        user.setOnline(isOnline);
        user.setLastSeen(ZonedDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<Long, List<UserDto>> searchUsers(
            @NonNull DefaultUserDetails userDetails,
            String search, int offset, int limit
    ) {

        log.debug("-> searchUsers(): search = {}, offset = {}, limit = {}", search, offset, limit);

        final String needle = foldForSearch(search);

        final List<UserEntity> matched = userRepository.findAllNotDeleted().stream()
                .filter(user -> !userDetails.id().equals(user.getId()))
                .filter(user -> foldForSearch(user.getUsername()).contains(needle)
                        || foldForSearch(user.getName()).contains(needle))
                .sorted(Comparator.comparing((UserEntity user) -> foldForSearch(user.getName())))
                .toList();

        return Pair.of(
                (long) matched.size(),
                matched.stream()
                        .skip(offset)
                        .limit(limit)
                        .map(user -> modelMapper.map(user, UserDto.class))
                        .toList()
        );
    }

    /** Chuẩn hóa để so khớp không phân biệt hoa thường và không phân biệt dấu tiếng Việt. */
    static String foldForSearch(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> getUsers(@NonNull List<Long> usersIds) {
        return userRepository.findAllById(usersIds)
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }

    @Transactional
    @SneakyThrows
    @Override
    public void changeUserAvatar(Long userId, @NonNull MultipartFile avatar) {
        final UserEntity user = findById(userId);
        final String filename = fileStorageService.storePicture(USER_AVATAR_PATH, valueOf(userId), avatar);
        user.setAvatarPath(filename);
    }

    @SneakyThrows
    @Override
    public byte[] getUserAvatar(Long userId) {
        final UserEntity user = findById(userId);
        if (user.getAvatarPath() == null) {
            throw new ResourceNotFoundException();
        }
        return fileStorageService.readFileAsBytes(Path.of(
                USER_AVATAR_PATH.toString(),
                valueOf(user.getId()),
                user.getAvatarPath()
        ));
    }

    @Transactional
    @Override
    public void deleteUserAvatar(Long userId) {
        final UserEntity user = findById(userId);
        user.setAvatarPath(null);
    }

}