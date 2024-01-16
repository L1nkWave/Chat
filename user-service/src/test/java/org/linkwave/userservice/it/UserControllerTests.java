package org.linkwave.userservice.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.RoleEntity.Roles;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.RoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.linkwave.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTests {

    private static final String REGISTRATION_ENDPOINT = "/api/v1/users/register";

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
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRespondWithBadCredentialsWhileRegistrationIfUsernameIsTaken() throws Exception {

        userRepository.save(UserEntity.builder()
                        .name(name)
                        .username(username)
                        .password(password)
                        .lastSeen(ZonedDateTime.now())
                        .avatarPath("")
                        .roles(List.of(roleRepository.findByName("ROLE_USER").get()))
                .build());

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
    void shouldRespondWithBadRequestIfAccessTokenIsNotPresent() throws Exception {
        userService.register(new UserRegisterRequest(username, password, name));

        mockMvc.perform(request(GET, "/api/v1/users"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRespondWithBadRequestIfAccessTokenIsNotValid() throws Exception {
        final String notValidAccessToken = "not_valid_access_token";

        mockMvc.perform(request(GET, "/api/v1/users").header(AUTHORIZATION, notValidAccessToken))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentType(APPLICATION_JSON)
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