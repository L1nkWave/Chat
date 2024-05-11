package org.linkwave.chatservice.api.users;

import org.linkwave.chatservice.common.RequestInitiator;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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

    default Map<Long, ContactDto> fetchAllContacts(@NonNull RequestInitiator initiator, int batchSize) {
        final String username = ""; // any username is matched
        int offset = 0;

        final List<ContactDto> allContacts = new LinkedList<>();
        List<ContactDto> contacts;

        do {
            contacts = getContacts(username, offset, batchSize, initiator.bearer());
            allContacts.addAll(contacts);
            offset += batchSize;
        } while (contacts.size() == batchSize);
        return allContacts.stream()
                .collect(toMap(contact -> contact.getUser().getId(), identity()));
    }

}
