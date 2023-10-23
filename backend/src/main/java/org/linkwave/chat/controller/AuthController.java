package org.linkwave.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.chat.dto.AuthDto;
import org.linkwave.chat.dto.UserLoginRequest;
import org.linkwave.chat.dto.UserRefreshRequest;
import org.linkwave.chat.dto.UserRegisterRequest;
import org.linkwave.chat.service.UserService;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(OK)
    public AuthDto register(@Valid @RequestBody UserRegisterRequest registerRequest) {
        return userService.register(registerRequest);
    }

    @PatchMapping("/login")
    @ResponseStatus(OK)
    public AuthDto login(@Valid @RequestBody UserLoginRequest loginRequest) {
        return userService.login(loginRequest);
    }

    @PatchMapping("/refresh")
    @ResponseStatus(OK)
    public AuthDto refresh(@Valid @RequestBody UserRefreshRequest refreshRequest) {
        return userService.refresh(refreshRequest);
    }

}