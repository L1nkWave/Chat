package org.linkwave.chat.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linkwave.chat.dto.AuthDto;
import org.linkwave.chat.dto.UserRegisterRequest;
import org.linkwave.chat.entity.RoleEntity;
import org.linkwave.chat.entity.RoleEntity.Roles;
import org.linkwave.chat.repository.RoleRepository;
import org.linkwave.chat.repository.UserRepository;
import org.linkwave.chat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTests {

    private static final String REGISTRATION_ENDPOINT = "/auth/register";
    private static final String LOGIN_ENDPOINT = "/auth/login";
    private static final String REFRESH_ENDPOINT = "/auth/refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private String username;
    private String password;
    private String name;

    @Transactional
    @BeforeEach
    void setUp() {

        // init fields
        username = "zookeeper";
        password = "zookeeper123";
        name = "Zookeeper";

        final String userRole = Roles.USER.getName();

        // save user role
        roleRepository.save(RoleEntity.builder()
                .name(userRole)
                .build()
        );
    }

    @Test
    void userShouldBeSuccessfullyRegistered() throws Exception {
        mockMvc.perform(request(POST, REGISTRATION_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "name": "%s"
                                }
                                """.formatted(username, password, name)))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(APPLICATION_JSON),
                        jsonPath("jwtAccess").isString(),
                        jsonPath("jwtRefresh").isString()
                );
    }

    @Test
    void shouldRespondWithBadCredentialsWhileRegistrationIfUsernameIsTaken() throws Exception {

        userService.register(new UserRegisterRequest(username, password, name));

        mockMvc.perform(request(POST, REGISTRATION_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "name": "%s"
                        }
                        """.formatted(username, password, name))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_JSON)
        );
    }

    @Test
    void shouldRespondWithBadCredentialsWhileRegistrationIfRequestBodyInvalid() throws Exception {

        final String notValidUsername = "us";
        final String notValidPassword = "";

        mockMvc.perform(request(POST, REGISTRATION_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "name": "%s"
                                }
                                """.formatted(notValidUsername, notValidPassword, name)))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentType(APPLICATION_JSON),
                        jsonPath("username").value("length should be from 3 to 32 characters"),
                        jsonPath("password").value("length should be from 3 to 64 characters")
                );
    }

    @Test
    void userShouldBeLoggedInWithValidCredentials() throws Exception {

        userService.register(new UserRegisterRequest(username, password, name));

        mockMvc.perform(request(PATCH, LOGIN_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
        ).andExpectAll(
                status().isOk(),
                content().contentType(APPLICATION_JSON),
                jsonPath("jwtAccess").isString(),
                jsonPath("jwtRefresh").isString()
        );
    }

    @Test
    void shouldRespondWithBadCredentialsWhileLoginIfUserNotExist() throws Exception {

        final String nonExistingUsername = "non_existing_username";

        mockMvc.perform(request(PATCH, LOGIN_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(nonExistingUsername, password))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_JSON)
        );
    }

    @Test
    void shouldRespondWithBadCredentialsWhileLoginIfPasswordIsIncorrect() throws Exception {
        userService.register(new UserRegisterRequest(username, password, name));

        final String incorrectPassword = "incorrect_password";

        mockMvc.perform(request(PATCH, LOGIN_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, incorrectPassword))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_JSON)
        );
    }

    @Test
    void shouldReturnNewJwtTokensIfRefreshTokenIsValid() throws Exception {
        final String validRefreshToken = userService
                .register(new UserRegisterRequest(username, password, name))
                .getJwtRefresh();

        mockMvc.perform(request(PATCH, REFRESH_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "jwtRefresh": "%s"
                        }
                        """.formatted(validRefreshToken))
        ).andExpectAll(
                status().isOk(),
                content().contentType(APPLICATION_JSON),
                jsonPath("jwtRefresh").isString()
        );
    }

    @Test
    void shouldRespondWithBadCredentialsIfRefreshTokenIsNotValid() throws Exception {
        userService.register(new UserRegisterRequest(username, password, name));

        final String notValidRefreshToken = "not_valid_refresh_token";

        mockMvc.perform(request(PATCH, REFRESH_ENDPOINT)
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "jwtRefresh": "%s"
                        }
                        """.formatted(notValidRefreshToken))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_JSON)
        );
    }

    @Test
    void shouldReturnPersonalInfoForLoggedInUser() throws Exception {
        AuthDto authDto = userService.register(new UserRegisterRequest(username, password, name));
        final String authHeader = format("Bearer %s", authDto.getJwtAccess());

        mockMvc.perform(request(GET, "/user").header(AUTHORIZATION, authHeader)
        ).andExpectAll(
                status().isOk(),
                content().contentType(APPLICATION_JSON),
                jsonPath("id").isNumber(),
                jsonPath("name").isString(),
                jsonPath("username").isString(),
                jsonPath("registeredAt").isString(),
                jsonPath("theme").isBoolean(),
                jsonPath("roles").isArray()
        );
    }

    @Test
    void shouldRespondWithForbiddenIfAccessTokenIsNotPresent() throws Exception {
        userService.register(new UserRegisterRequest(username, password, name));

        mockMvc.perform(request(GET, "/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRespondWithForbiddenIfAccessTokenIsNotValid() throws Exception {
        final String notValidAccessToken = "not_valid_access_token";

        mockMvc.perform(request(GET, "/user").header(AUTHORIZATION, notValidAccessToken))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().contentType(TEXT_PLAIN)
                );
    }

    @AfterEach
    @Transactional
    void clear() {
        userRepository.dropUserRoles();
        roleRepository.deleteAll();
        userRepository.deleteAll();
    }

}