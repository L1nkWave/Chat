package org.linkwave.userservice.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.entity.RoleEntity;
import org.linkwave.userservice.entity.RoleEntity.Roles;
import org.linkwave.userservice.entity.UserEntity;
import org.linkwave.userservice.repository.RoleRepository;
import org.linkwave.userservice.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static org.linkwave.userservice.utils.TokenGenerator.generateToken;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTests {

    private static final String OPENAPI_PATH = "static/openapi.json";
    private static final String BASE_URL = "/api/v1/users";
    private static final String REGISTRATION_ENDPOINT = format("%s/register", BASE_URL);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

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

        persistUser(name, username);

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
        mockMvc.perform(request(GET, BASE_URL)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRespondWithBadRequestIfAccessTokenIsNotValid() throws Exception {
        final String notValidAccessToken = "not_valid_access_token";

        mockMvc.perform(request(GET, BASE_URL).header(AUTHORIZATION, notValidAccessToken))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentType(APPLICATION_JSON)
                );
    }

    @Test
    void shouldGetUserById() throws Exception {

        final Long userId = persistUser(name, username).getId();
        final String url = format("%s/{id}", BASE_URL);
        final String token = format("Bearer %s", generateToken(userId, username, ofMinutes(1L)));

        mockMvc.perform(get(url, userId).header(AUTHORIZATION, token))
                .andExpectAll(
                        status().isOk(),
                        openApi().isValid(OPENAPI_PATH)
                );
    }

    @Disabled("Function STARTS_WITH is not supported in H2")
    @Test
    void shouldRetrieveUsersWithUsernameAndOffsetAndLimit() throws Exception {

        final int usersCount = 10;
        final List<UserDto> users = generateUsersWithPersistence(usersCount);

        final int offset = 5, limit = 5;
        final String matchUsername = "user";
        final String expectedContent = objectMapper.writeValueAsString(users.stream()
                .skip(offset)
                .limit(limit)
                .toList()
        );
        final String token = format("Bearer %s", generateToken(
                users.get(0).getId(),
                users.get(0).getUsername(),
                ofMinutes(1L))
        );

        mockMvc.perform(get(BASE_URL).header(AUTHORIZATION, token)
                .param("username", matchUsername)
                .param("offset", String.valueOf(offset))
                .param("limit", String.valueOf(limit))
        ).andExpectAll(
                status().isOk(),
                openApi().isValid(OPENAPI_PATH),
                content().contentType(APPLICATION_JSON),
                content().json(expectedContent)
        );
    }

    @AfterEach
    @Transactional
    void clear() {
        userRepository.dropUserRoles();
        roleRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UserEntity persistUser(String name, String username) {
        UserEntity newUser = UserEntity.builder()
                .name(name)
                .username(username)
                .password(password)
                .lastSeen(ZonedDateTime.now())
                .roles(List.of(roleRepository.findByName(Roles.USER.getName()).get()))
                .build();

        return userRepository.save(newUser);
    }

    private List<UserDto> generateUsersWithPersistence(final int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    final var _username = format("@user%d", i);
                    final var name = format("User%d", i);
                    return persistUser(name, _username);
                })
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }

}