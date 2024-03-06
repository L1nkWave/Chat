package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.service.ContactService;
import org.linkwave.userservice.service.UserService;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ContactService contactService;

    @PostMapping("/register")
    @ResponseStatus(CREATED)
    public void register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/contacts")
    public List<ContactDto> getContacts(@RequestParam String username,
                                        @RequestParam int offset, @RequestParam int limit,
                                        @NonNull HttpServletResponse response) {
        final Pair<Integer, List<ContactDto>> result = contactService.getContactsByUsername(getDetails().id(), username, offset, limit);
        response.setHeader(TOTAL_COUNT.getValue(), String.valueOf(result.getFirst()));
        return result.getSecond();
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