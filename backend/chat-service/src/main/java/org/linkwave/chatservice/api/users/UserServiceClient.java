package org.linkwave.chatservice.api.users;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(url = "http://localhost:8082/api/v1/users", value = "user-service")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable Long userId, @RequestHeader("Authorization") String authHeader);

}
