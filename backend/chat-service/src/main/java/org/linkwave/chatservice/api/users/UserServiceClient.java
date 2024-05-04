package org.linkwave.chatservice.api.users;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserDto getUser(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader
    );

    @GetMapping("/batch")
    List<UserDto> getUsers(
            @RequestParam List<Long> usersIds,
            @RequestHeader("Authorization") String authHeader
    );

    @GetMapping("/contacts")
    List<ContactDto> getContacts(
            @RequestParam String username,
            @RequestParam int offset, @RequestParam int limit,
            @RequestHeader("Authorization") String authHeader
    );

}
