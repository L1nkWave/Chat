package org.linkwave.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.linkwave.auth.dto.TokensDto;
import org.linkwave.auth.entity.Role;
import org.linkwave.auth.entity.Role.Roles;
import org.linkwave.auth.entity.User;
import org.linkwave.auth.repository.RoleRepository;
import org.linkwave.auth.repository.UserRepository;
import org.linkwave.auth.security.utils.Cookies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Optional;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApplicationTests {

    private static final String BASE_URL = "http://localhost:8081/api/v1/auth";
    private static final String OPENAPI_PATH = "static/openapi.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "Refresh-Token";
    private static final String USERNAME = "@emmtlor";
    private static final String PASSWORD = "qwerty123";

    @Transactional
    @BeforeEach
    void prepare() {
        final Role userRole = roleRepository.save(Role.builder()
                .name(Roles.USER.getName())
                .build());

        final User user = User.builder()
                .username(USERNAME)
                .password(passwordEncoder.encode(PASSWORD))
                .build();

        user.addRole(userRole);
        userRepository.save(user);
    }

    @Transactional
    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Should login with correct credentials")
    void shouldLogInWithCorrectCredentials() throws Exception {
        mockMvc.perform(POST("/login")
                .contentType(APPLICATION_JSON)
                .content(format("""
                           {
                          		"username": "%s",
                        		"password": "%s"
                           }
                        """, USERNAME, PASSWORD))
        ).andExpectAll(
                status().isOk(),
                cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true),
                cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, (int) Duration.ofHours(1).toSeconds()),
                content().contentType(APPLICATION_JSON),
                openApi().isValid(OPENAPI_PATH)
        );

    }

    @Test
    @DisplayName("Should reject login with bad credentials")
    void shouldRejectLogInWithBadCredentials() throws Exception {

        final var wrongPassword = "123";

        // request with non-empty body
        mockMvc.perform(POST("/login")
                .contentType(APPLICATION_JSON)
                .content(format("""
                           {
                          		"username": "%s",
                        		"password": "%s"
                           }
                        """, USERNAME, wrongPassword))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_JSON),
                openApi().isValid(OPENAPI_PATH)
        );

        // request with empty body
        mockMvc.perform(POST("/login")
                .contentType(APPLICATION_JSON)
                .content("")
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_JSON),
                openApi().isValid(OPENAPI_PATH)
        );

    }

    @Test
    @DisplayName("Should log out with valid access token")
    void shouldLogOutWithValidAccessToken() throws Exception {

        // login in order to retrieve a pair of tokens
        mockMvc.perform(POST("/login")
                .contentType(APPLICATION_JSON)
                .content(format("""
                           {
                          		"username": "%s",
                        		"password": "%s"
                           }
                        """, USERNAME, PASSWORD))
        ).andDo(result -> {
            // perform log out request
            mockMvc.perform(POST("/logout").header(AUTHORIZATION, getAccessToken(result)))
                    .andExpectAll(
                            status().isNoContent(),
                            openApi().isValid(OPENAPI_PATH)
                    );
        });
    }

    @Test
    @DisplayName("Should reject log out with invalid access token")
    void shouldRejectLogOutWithInvalidAccessToken() throws Exception {
        final var accessToken = "Bearer ...";

        mockMvc.perform(POST("/logout").header(AUTHORIZATION, accessToken))
                .andExpectAll(
                        status().isBadRequest(),
                        openApi().isValid(OPENAPI_PATH)
                );
    }

    @Test
    @DisplayName("Should refresh tokens with valid refresh token as cookie")
    void shouldRefreshTokensWithValidRefreshTokenAsCookie() throws Exception {

        // login in order to retrieve a pair of tokens
        mockMvc.perform(POST("/login")
                .contentType(APPLICATION_JSON)
                .content(format("""
                           {
                          		"username": "%s",
                        		"password": "%s"
                           }
                        """, USERNAME, PASSWORD))
        ).andDo(result -> {
            final Optional<Cookie> optionalCookie = stream(result.getResponse().getCookies())
                    .filter(cookie -> cookie.getName().equals(REFRESH_TOKEN_COOKIE_NAME))
                    .findAny();

            // if NoSuchElementException will be thrown test won't pass
            final Cookie cookie = optionalCookie.get();

            // perform refresh tokens request
            mockMvc.perform(POST("/refresh-tokens").cookie(cookie))
                    .andExpectAll(
                            status().isOk(),
                            cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true),
                            cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, (int) Duration.ofHours(1).toSeconds()),
                            content().contentType(APPLICATION_JSON),
                            openApi().isValid(OPENAPI_PATH)
                    );
        });

    }

    @Test
    @DisplayName("Should reject refresh tokens with empty cookie")
    void shouldRejectRefreshTokensWithEmptyCookie() throws Exception {
        mockMvc.perform(POST("/refresh-tokens"))
                .andExpectAll(
                        status().isUnauthorized(),
                        content().contentType(APPLICATION_JSON),
                        openApi().isValid(OPENAPI_PATH)
                );
    }

    @Test
    @DisplayName("Check refresh token unavailability")
    void checkRefreshTokenUnavailability() throws Exception {

        final var wrongRefreshTokenValue = "...";
        final var refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, wrongRefreshTokenValue);
        refreshCookie.setPath("/api/v1/auth/refresh-tokens");
        refreshCookie.setMaxAge((int) Duration.ofHours(1).toSeconds()); // 3600 seconds to live
        refreshCookie.setHttpOnly(true);
        refreshCookie.setAttribute("Strict-Site", Cookies.STRICT_SAME_SITE);

        mockMvc.perform(POST("/refresh-tokens").cookie(refreshCookie))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentType(APPLICATION_JSON),
                        openApi().isValid(OPENAPI_PATH)
                );
    }

    @Test
    @DisplayName("Should validate access token with success")
    void shouldValidateAccessTokenWithSuccess() throws Exception {

        // login in order to retrieve a pair of tokens
        mockMvc.perform(POST("/login")
                .contentType(APPLICATION_JSON)
                .content(format("""
                           {
                          		"username": "%s",
                        		"password": "%s"
                           }
                        """, USERNAME, PASSWORD))
        ).andDo(result -> mockMvc
                .perform(GET("/validate-token").header(AUTHORIZATION, getAccessToken(result)))
                .andExpectAll(
                        status().isNoContent(),
                        openApi().isValid(OPENAPI_PATH)
                )
        );
    }

    @Test
    @DisplayName("Should reject request if token is invalid")
    void shouldRejectRequestIfTokenIsInvalid() throws Exception {

        final var invalidAccessToken = "Bearer ...";

        mockMvc.perform(GET("/validate-token").header(AUTHORIZATION, invalidAccessToken))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentType(APPLICATION_JSON),
                        openApi().isValid(OPENAPI_PATH)
                );
    }

    private String getAccessToken(@NonNull MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        final var tokensDto = objectMapper.readValue(result.getResponse().getContentAsString(), TokensDto.class);
        return format("Bearer %s", tokensDto.accessToken());
    }

    @NonNull
    private static MockHttpServletRequestBuilder GET(String endPoint) {
        return get(format("%s/%s", BASE_URL, endPoint));
    }

    @NonNull
    private static MockHttpServletRequestBuilder POST(String endPoint) {
        return post(format("%s/%s", BASE_URL, endPoint));
    }

}
