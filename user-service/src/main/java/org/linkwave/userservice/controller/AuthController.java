package org.linkwave.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.userservice.dto.AuthDto;
import org.linkwave.userservice.dto.UserLoginRequest;
import org.linkwave.userservice.dto.UserRefreshRequest;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.service.UserService;
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