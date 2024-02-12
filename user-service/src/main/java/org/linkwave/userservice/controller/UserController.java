package org.linkwave.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.security.DefaultUserDetails;
import org.linkwave.userservice.service.UserService;
import org.springframework.data.util.Pair;
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
    public List<UserDto> getUsers(@RequestParam String username,
                                  @RequestParam int offset, @RequestParam int limit,
                                  @NonNull HttpServletResponse response) {
        final Pair<Long, List<UserDto>> result = userService.getUsersByUsername(getDetails(), username, offset, limit);
        response.setHeader(TOTAL_COUNT.getValue(), String.valueOf(result.getFirst()));
        return result.getSecond();
    }

    private DefaultUserDetails getDetails() {
        return (DefaultUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}