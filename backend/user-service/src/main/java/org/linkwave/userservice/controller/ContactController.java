package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.NewContactRequest;
import org.linkwave.userservice.service.ContactService;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;

@RestController
@RequestMapping("/api/v1/users/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public List<ContactDto> getContacts(@RequestParam String username,
                                        @RequestParam int offset, @RequestParam int limit,
                                        @NonNull HttpServletResponse response) {
        final Pair<Integer, List<ContactDto>> result = contactService.getContactsByUsername(getDetails().id(), username, offset, limit);
        response.setHeader(TOTAL_COUNT.getValue(), String.valueOf(result.getFirst()));
        return result.getSecond();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addContact(@Valid @RequestBody NewContactRequest newContact) {
        contactService.addContact(getDetails().id(), newContact);
    }

    @DeleteMapping("/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContact(@PathVariable Long contactId) {
        contactService.removeContact(getDetails().id(), contactId);
    }

    private DefaultUserDetails getDetails() {
        return (DefaultUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}