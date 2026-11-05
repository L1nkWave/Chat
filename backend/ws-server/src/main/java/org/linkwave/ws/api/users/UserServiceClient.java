package org.linkwave.ws.api.users;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @PostMapping("/{id}/online")
    void updateUserStatus(@PathVariable Long id, @RequestParam Boolean value);

}