package org.linkwave.userservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.linkwave.shared.auth.DefaultUserDetails;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.NewContactRequest;
import org.linkwave.userservice.service.ContactService;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.shared.utils.Headers.TOTAL_COUNT;
import static org.linkwave.userservice.controller.ConstraintErrorMessages.PAGINATION_PARAM_MAX_MSG;
import static org.linkwave.userservice.controller.ConstraintErrorMessages.PAGINATION_PARAM_MIN_MSG;

@Validated
@RestController
@RequestMapping("/api/v1/users/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public List<ContactDto> getContacts(
            @NonNull HttpServletResponse response,
            @RequestParam String search,
            @Min(value = 0, message = PAGINATION_PARAM_MIN_MSG) @Max(value = 100, message = PAGINATION_PARAM_MAX_MSG)
            @RequestParam int offset,
            @Min(value = 0, message = PAGINATION_PARAM_MIN_MSG) @Max(value = 100, message = PAGINATION_PARAM_MAX_MSG)
            @RequestParam int limit
    ) {
        final Pair<Integer, List<ContactDto>> result = contactService.getContactsBySearch(getDetails().id(), search, offset, limit);
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
