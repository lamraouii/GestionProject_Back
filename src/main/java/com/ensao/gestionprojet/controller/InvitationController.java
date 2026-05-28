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
}