package org.linkwave.userservice.unit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.shared.storage.LocalFileStorageService;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.exception.ResourceNotFoundException;
import org.linkwave.userservice.repository.RoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.service.UserService;
import org.linkwave.userservice.service.impl.DefaultUserService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.linkwave.userservice.entity.RoleEntity.Roles.USER;
import static org.linkwave.userservice.utils.UsersUtils.generateUsers;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    private static final RoleEntity USER_ROLE = RoleEntity.builder()
            .id(1)
            .name(USER.getValue())
            .build();

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private ModelMapper modelMapper;

    private UserService userService;

    private String username;
    private String password;
    private String name;

    @BeforeEach
    void setUp() {
        // prepare user service
        var passwordEncoder = new BCryptPasswordEncoder();
        modelMapper = new ModelMapper();
        userService = new DefaultUserService(
                userRepository, roleRepository,
                new LocalFileStorageService(),
                passwordEncoder, modelMapper
        );

        // initialize fields
        username = "zookeeper";
        password = "zookeeper123";
        name = "Zookeeper";
    }

    @Test
    void userShouldBeSuccessfullyRegistered() {
        var newUser = UserEntity.builder()
                .name(name).username(username)
                .password(password).roles(List.of(USER_ROLE))
                .build();

        var savedUser = UserEntity.builder()
                .id(1L).name(name).username(username)
                .password(password).roles(List.of(USER_ROLE))
                .build();

        USER_ROLE.getUsers().add(savedUser);


        when(roleRepository.findByName(USER.getValue())).thenReturn(Optional.of(USER_ROLE));
        when(userRepository.findByUsername(username)).thenReturn(empty());
        when(userRepository.save(newUser)).thenReturn(savedUser);

        userService.register(new UserRegisterRequest(username, password, name));

        verify(roleRepository, times(1)).findByName(USER.getValue());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    void shouldThrowExceptionWhileRegistrationWhenUsernameIsTaken() {
        var existingUser = UserEntity.builder()
                .id(1L).name(name).username(username)
                .password(password).roles(List.of(USER_ROLE))
                .build();

        USER_ROLE.getUsers().add(existingUser);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));

        assertThrows(
                BadCredentialsException.class,
                () -> userService.register(new UserRegisterRequest(username, password, name))
        );

        verify(roleRepository, never()).findByName(any());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find existing user by id")
    void shouldFindExistingUserById() {
        final var existingUser = UserEntity.builder()
                .id(1L).name(name).username(username)
                .password(password).roles(List.of(USER_ROLE))
                .build();

        final Long userId = existingUser.getId();
        USER_ROLE.getUsers().add(existingUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        final UserEntity user = userService.findById(userId);

        assertThat(user).isEqualTo(existingUser);
    }

    @Test
    @DisplayName("Should throw exception when find user by id if not exist")
    void shouldThrowExceptionWhenFindUserByIdIfNotExist() {
        final var nonExistingId = 1L;

        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(nonExistingId));
    }

    @Test
    @DisplayName("Should get user info by id")
    void shouldGetUserInfoById() {

        final var createdAt = ZonedDateTime.now();

        final var existingUser = UserEntity.builder()
                .id(1L)
                .name(name)
                .username(username)
                .password(password)
                .createdAt(createdAt)
                .roles(List.of(USER_ROLE))
                .bio("Test")
                .isOnline(true)
                .build();

        final var lastSeen = existingUser.getLastSeen();
        final Long userId = existingUser.getId();
        USER_ROLE.getUsers().add(existingUser);

        final UserDto expectedUserDto = UserDto.builder()
                .id(1L)
                .name(name)
                .username(username)
                .createdAt(createdAt)
                .lastSeen(lastSeen)
                .bio("Test")
                .isOnline(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        final UserDto userDto = userService.getUser(userId);

        assertThat(userDto).isNotNull();
        assertThat(userDto).isEqualTo(expectedUserDto);
    }

    @Test
    @DisplayName("Should find users by username with offset and limit")
    void shouldFindUsersByUsernameWithOffsetAndLimit() {
        final var userDetails = DefaultUserDetails.builder()
                .id(1L)
                .username(username)
                .authorities(List.of(new SimpleGrantedAuthority(USER_ROLE.getName())))
                .build();

        final int usersCount = 10;
        final int offset = 5;
        final int limit = 5;

        final String username = "toxic";
        final String usernamePattern = "%toxic%";
        final List<UserEntity> users = generateUsers(usersCount, usernamePattern, USER_ROLE);
        final List<UserEntity> expectedUsers = users.stream()
                .skip(offset)
                .limit(5)
                .toList();

        final List<UserDto> expectedUsersDto = expectedUsers.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();

        when(userRepository.getUsersByUsernameContains(userDetails.id(), usernamePattern, offset, limit))
                .thenReturn(expectedUsers);

        when(userRepository.getUsersCountByUsernameContains(userDetails.id(), usernamePattern))
                .thenReturn((long) users.size());

        final Pair<Long, List<UserDto>> result = userService.getUsersByUsernameWithoutContacts(userDetails, username, offset, limit);

        assertThat(result).isNotNull();
        assertThat(result.getFirst()).isEqualTo(users.size());
        assertThat(result.getSecond()).isNotEmpty();
        assertThat(result.getSecond()).isEqualTo(expectedUsersDto);

        verify(userRepository, times(1)).getUsersByUsernameContains(userDetails.id(), usernamePattern, offset, limit);
        verify(userRepository, times(1)).getUsersCountByUsernameContains(userDetails.id(), usernamePattern);
    }

    @AfterEach
    void cleanUp() {
        USER_ROLE.getUsers().clear();
    }

}
