package org.linkwave.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.security.DefaultUserDetails;
import org.linkwave.userservice.service.UserService;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(CREATED)
    public void register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
    }

    @GetMapping
    public UserDto getPersonalInfo(@NonNull @AuthenticationPrincipal DefaultUserDetails details) {
        return userService.getPersonalInfo(details.id());
    }

}