package org.linkwave.chatservice.invitation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.linkwave.chatservice.common.RequestUtils.requestInitiator;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/{id}/invitations")
    @ResponseStatus(CREATED)
    public InvitationDto invite(@PathVariable String id,
                                @RequestBody @Valid InviteRequest body,
                                @NonNull HttpServletRequest request) {
        return invitationService.invite(requestInitiator(request), id, body.username());
    }

    @GetMapping("/invitations")
    public List<InvitationDto> listMyInvitations(@NonNull HttpServletRequest request) {
        return invitationService.listPending(requestInitiator(request));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @ResponseStatus(NO_CONTENT)
    public void accept(@PathVariable String invitationId, @NonNull HttpServletRequest request) {
        invitationService.accept(requestInitiator(request), invitationId);
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @ResponseStatus(NO_CONTENT)
    public void decline(@PathVariable String invitationId, @NonNull HttpServletRequest request) {
        invitationService.decline(requestInitiator(request), invitationId);
    }

}