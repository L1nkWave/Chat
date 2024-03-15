package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.dto.UserRegisterRequest;
import org.linkwave.userservice.exception.LimitExceededException;
import org.linkwave.userservice.exception.UnacceptableRequestDataException;
import org.linkwave.userservice.service.ContactService;
import org.linkwave.userservice.service.UserService;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static java.lang.String.format;
import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    public static final int BATCH_SIZE_LIMIT = 100;

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
        final Pair<Long, List<UserDto>> result = userService.getUsersByUsernameWithoutContacts(getDetails(), username, offset, limit);
        response.setHeader(TOTAL_COUNT.getValue(), String.valueOf(result.getFirst()));
        return result.getSecond();
    }

    @GetMapping("/batch")
    public List<UserDto> getUsers(@RequestParam List<Long> usersIds) {
        if (usersIds.size() > BATCH_SIZE_LIMIT) {
            throw new LimitExceededException(
                    format("Allowed to load maximum %d users per request", BATCH_SIZE_LIMIT)
            );
        }
        return userService.getUsers(usersIds);
    }

    @PostMapping("/avatar")
    @ResponseStatus(CREATED)
    public void uploadAvatar(@NonNull @RequestPart("file") MultipartFile[] files) {
        log.debug("-> uploadAvatar(): files {}", files.length);
        if (files.length > 1) {
            throw new UnacceptableRequestDataException("Only one file can be uploaded per request");
        }

        final MultipartFile targetFile = files[0];

        if (targetFile.isEmpty()) {
            throw new UnacceptableRequestDataException("Uploaded file can not be empty");
        }
        userService.changeUserAvatar(getDetails().id(), targetFile);
    }

    @DeleteMapping("/avatar")
    public void deleteAvatar() {
        userService.deleteUserAvatar(getDetails().id());
    }

    @GetMapping(
            value = "/{id}/avatar",
            produces = {
                    IMAGE_PNG_VALUE,
                    IMAGE_GIF_VALUE,
                    IMAGE_JPEG_VALUE,
                    "image/jpg",
                    "image/webp"
            })
    public byte[] getAvatar(@PathVariable Long id) {
        return userService.getUserAvatar(id);
    }

    private DefaultUserDetails getDetails() {
        return (DefaultUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}