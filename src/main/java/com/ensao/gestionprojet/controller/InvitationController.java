package com.ensao.gestionprojet.controller;


import com.ensao.gestionprojet.dto.InviteMemberRequestDto;
import com.ensao.gestionprojet.service.InvitationService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> inviter(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberRequestDto request
    ) {

        invitationService.inviterMembre(id, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{id}/accept")
    public ResponseEntity<String> accepterInvitation(
            @PathVariable Long id
    ) {

        invitationService.accepterInvitation(id);

        return ResponseEntity.ok("Invitation acceptée");
    }

    @PostMapping("/invitations/{id}/reject")
    public ResponseEntity<String> refuserInvitation(
            @PathVariable Long id
    ) {

        invitationService.refuserInvitation(id);

        return ResponseEntity.ok("Invitation refusée");
    }
}