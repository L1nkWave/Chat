package org.linkwave.chat.controller;

import lombok.RequiredArgsConstructor;
import org.linkwave.chat.dto.UserDto;
import org.linkwave.chat.security.DefaultUserDetails;
import org.linkwave.chat.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserDto getPersonalInfo() {
        var userDetails = (DefaultUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return userService.getPersonalInfo(userDetails.getId());
    }

}