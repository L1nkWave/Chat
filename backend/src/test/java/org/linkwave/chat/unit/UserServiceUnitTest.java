package org.linkwave.chat.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.chat.dto.AuthDto;
import org.linkwave.chat.dto.UserLoginRequest;
import org.linkwave.chat.dto.UserRefreshRequest;
import org.linkwave.chat.dto.UserRegisterRequest;
import org.linkwave.chat.entity.RoleEntity;
import org.linkwave.chat.entity.UserEntity;
import org.linkwave.chat.repository.RoleRepository;
import org.linkwave.chat.repository.UserRepository;
import org.linkwave.chat.security.JwtProvider;
import org.linkwave.chat.service.UserService;
import org.linkwave.chat.service.impl.DefaultUserService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.linkwave.chat.security.Token.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private JwtProvider jwtProvider;

	private UserService userService;
	private PasswordEncoder passwordEncoder;

	private String username;
	private String password;
	private String name;
	private String userRole;

	@BeforeEach
	void setUp() {
		// inject fields of jwt provider
		setField(jwtProvider, "subject", "link-wave");
		setField(jwtProvider, "issuer", "link-wave-issuer");
		setField(jwtProvider, "accessExpiration", 60);
		setField(jwtProvider, "refreshExpiration", 30);
		setField(jwtProvider, "accessSecretKey", "access-secret-key");
		setField(jwtProvider, "refreshSecretKey", "refresh-secret-key");

		// prepare user service
		passwordEncoder = new BCryptPasswordEncoder();
		userService = new DefaultUserService(userRepository, roleRepository, passwordEncoder, jwtProvider);

		// initialize fields
		username = "zookeeper";
		password = "zookeeper123";
		name = "Zookeeper";
		userRole = "ROLE_USER";
	}

	@Test
	void userShouldBeSuccessfullyRegistered() {
		var role = RoleEntity.builder()
				.id(1).name(userRole).users(new ArrayList<>())
				.build();

		var newUser = UserEntity.builder()
				.name(name).username(username)
				.password(password).roles(List.of(role))
				.build();

		var savedUser = UserEntity.builder()
				.id(1L).name(name).username(username)
				.password(password).roles(List.of(role))
				.build();

		final String accessToken = jwtProvider.generateToken(savedUser, ACCESS);
		final String refreshToken = jwtProvider.generateToken(savedUser, REFRESH);

		var expectedAuthDto = new AuthDto(accessToken, refreshToken);

		when(roleRepository.findByName(userRole)).thenReturn(Optional.of(role));
		when(userRepository.findByUsername(username)).thenReturn(empty());
		when(userRepository.save(newUser)).thenReturn(savedUser);
		when(jwtProvider.generateToken(savedUser, ACCESS)).thenReturn(accessToken);
		when(jwtProvider.generateToken(savedUser, REFRESH)).thenReturn(refreshToken);

		AuthDto actualAuthDto = userService.register(new UserRegisterRequest(username, password, name));

		assertThat(actualAuthDto).isEqualTo(expectedAuthDto);

		verify(roleRepository, times(1)).findByName(userRole);
		verify(userRepository, times(1)).findByUsername(username);
		verify(userRepository, times(1)).save(newUser);
	}

	@Test
	void shouldThrowExceptionWhileRegistrationWhenUsernameIsTaken() {
		var role = RoleEntity.builder()
				.id(1).name(userRole).users(Collections.emptyList())
				.build();

		var existingUser = UserEntity.builder()
				.id(1L).name(name).username(username)
				.password(password).roles(List.of(role))
				.build();

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
	void userShouldBeSuccessfullyLoggedInWithCorrectCredentials() {
		var role = RoleEntity.builder()
				.id(1).name(userRole).users(new ArrayList<>())
				.build();

		var existingUser = UserEntity.builder()
				.id(1L).name(name).username(username)
				.password(passwordEncoder.encode(password)).roles(List.of(role))
				.build();

		final String accessToken = jwtProvider.generateToken(existingUser, ACCESS);
		final String refreshToken = jwtProvider.generateToken(existingUser, REFRESH);

		var expectedAuthDto = new AuthDto(accessToken, refreshToken);

		when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
		when(jwtProvider.generateToken(existingUser, ACCESS)).thenReturn(accessToken);
		when(jwtProvider.generateToken(existingUser, REFRESH)).thenReturn(refreshToken);

		AuthDto actualAuthDto = userService.login(new UserLoginRequest(username, password));

		assertThat(actualAuthDto).isEqualTo(expectedAuthDto);

		verify(roleRepository, never()).findByName(userRole);
		verify(userRepository, only()).findByUsername(username);
	}

	@Test
	void shouldThrowExceptionWhileLoginIfPasswordIsIncorrect() {
		var role = RoleEntity.builder()
				.id(1).name(userRole).users(new ArrayList<>())
				.build();

		var existingUser = UserEntity.builder()
				.id(1L).name(name).username(username)
				.password(passwordEncoder.encode(password)).roles(List.of(role))
				.build();

		final String incorrectPassword = "incorrect_password";

		when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
		assertThrows(
				BadCredentialsException.class,
				() -> userService.login(new UserLoginRequest(username, incorrectPassword))
		);

		verify(jwtProvider, never()).generateToken(any(), any());
		verify(userRepository, only()).findByUsername(username);
	}

	@Test
	void shouldThrowExceptionWhileLoginIfUsernameNotExist() {
		final String nonExistingUsername = "non_existing_username";

		when(userRepository.findByUsername(nonExistingUsername)).thenReturn(empty());
		assertThrows(
				BadCredentialsException.class,
				() -> userService.login(new UserLoginRequest(nonExistingUsername, password))
		);

		verify(jwtProvider, never()).generateToken(any(), any());
		verify(userRepository, only()).findByUsername(nonExistingUsername);
	}

	@Test
	void shouldReturnJwtPairIfRefreshTokenExists() {
		var role = RoleEntity.builder()
				.id(1).name(userRole).users(new ArrayList<>())
				.build();

		final String initialRefreshToken = "initial_refresh_token";

		var existingUser = UserEntity.builder()
				.id(1L).name(name).username(username).refreshToken(initialRefreshToken)
				.password(passwordEncoder.encode(password)).roles(List.of(role))
				.build();

		final String accessToken = jwtProvider.generateToken(existingUser, ACCESS);
		final String refreshToken = jwtProvider.generateToken(existingUser, REFRESH);

		var expectedAuthDto = new AuthDto(accessToken, refreshToken);

		when(userRepository.findByRefreshToken(initialRefreshToken)).thenReturn(Optional.of(existingUser));
		when(jwtProvider.generateToken(existingUser, ACCESS)).thenReturn(accessToken);
		when(jwtProvider.generateToken(existingUser, REFRESH)).thenReturn(refreshToken);

		var actualAuthDto = userService.refresh(new UserRefreshRequest(initialRefreshToken));

		assertThat(actualAuthDto).isEqualTo(expectedAuthDto);

		verify(userRepository, only()).findByRefreshToken(initialRefreshToken);
	}

	@Test
	void shouldThrowExceptionWhileRefreshIfRefreshTokenNotExist() {
		final String nonExistingRefreshToken = "non_existing_refresh_token";

		when(userRepository.findByRefreshToken(nonExistingRefreshToken)).thenReturn(empty());

		assertThrows(
				BadCredentialsException.class,
				() -> userService.refresh(new UserRefreshRequest(nonExistingRefreshToken))
		);

		verify(userRepository, only()).findByRefreshToken(nonExistingRefreshToken);
	}

}
