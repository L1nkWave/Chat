package org.linkwave.userservice.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.RoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.UserService;
import org.linkwave.userservice.service.impl.DefaultUserService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	private UserService userService;

	private String username;
	private String password;
	private String name;
	private String userRole;

	@BeforeEach
	void setUp() {
		// prepare user service
		var passwordEncoder = new BCryptPasswordEncoder();
		var modelMapper = new ModelMapper();
		userService = new DefaultUserService(userRepository, roleRepository, passwordEncoder, modelMapper);

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


		when(roleRepository.findByName(userRole)).thenReturn(Optional.of(role));
		when(userRepository.findByUsername(username)).thenReturn(empty());
		when(userRepository.save(newUser)).thenReturn(savedUser);

		userService.register(new UserRegisterRequest(username, password, name));

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

}
