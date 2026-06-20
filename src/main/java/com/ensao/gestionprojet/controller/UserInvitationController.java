package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.InvitationResponseDto;
import com.ensao.gestionprojet.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class UserInvitationController {

    private final InvitationService invitationService;

    @GetMapping("/my")
    public ResponseEntity<List<InvitationResponseDto>> getMesInvitations() {
        return ResponseEntity.ok(invitationService.getMesInvitations());
    }

    @PatchMapping("/{invitationKey}/accept")
    public ResponseEntity<String> accepterInvitation(
            @PathVariable String invitationKey
    ) {
        invitationService.accepterInvitation(invitationKey);
        return ResponseEntity.ok("Invitation acceptee");
    }

    @PatchMapping("/{invitationKey}/reject")
    public ResponseEntity<String> refuserInvitation(
            @PathVariable String invitationKey
    ) {
        invitationService.refuserInvitation(invitationKey);
        return ResponseEntity.ok("Invitation refusee");
    }
}
