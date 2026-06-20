package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.CreateProjetRequestDto;
import com.ensao.gestionprojet.dto.InviteMembreProjetRequestDto;
import com.ensao.gestionprojet.dto.MemberResponseDto;
import com.ensao.gestionprojet.dto.ProjetResponseDto;
import com.ensao.gestionprojet.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class ProjetController {

    private final ProjetService projetService;
    // Creer un projet personnel ou d'entreprise
    @PostMapping
    public ResponseEntity<ProjetResponseDto> creerProjet(
            @Valid @RequestBody CreateProjetRequestDto request
    ) {
        ProjetResponseDto response = projetService.creerProjet(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Valider un projet d'entreprise (ADMIN)
    @PutMapping("/{id}/valider")
    public ResponseEntity<ProjetResponseDto> validerProjet(
            @PathVariable Long id
    ) {
        ProjetResponseDto response = projetService.validerProjet(id);
        return ResponseEntity.ok(response);
    }

   // Rejeter un projet d'entreprise (ADMIN)
    @PutMapping("/{id}/rejeter")
    public ResponseEntity<ProjetResponseDto> rejeterProjet(
            @PathVariable Long id
    ) {
        ProjetResponseDto response = projetService.rejeterProjet(id);
        return ResponseEntity.ok(response);
    }

    // Récuperer les détails d'un projet par ID
    @GetMapping("/{id}")
    public ResponseEntity<ProjetResponseDto> getProjet(
            @PathVariable Long id
    ) {
        ProjetResponseDto response = projetService.getProjet(id);
        return ResponseEntity.ok(response);
    }

    // Inviter un membre au projet (MANAGER)
    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> inviterMembreProjet(
            @PathVariable Long id,
            @Valid @RequestBody InviteMembreProjetRequestDto request
    ) {
        projetService.inviterMembreProjet(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponseDto>> getMembresProjet(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                projetService.getMembresProjet(id)
        );
    }

    /**
     * Récupérer tous les projets de l'utilisateur courant
     */
    @GetMapping("/mes-projets")
    public ResponseEntity<List<ProjetResponseDto>> getMesProjets() {
        List<ProjetResponseDto> response = projetService.getMesProjets();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entreprise/{entrepriseId}")
    public ResponseEntity<List<ProjetResponseDto>> getProjetsEntreprise(
            @PathVariable Long entrepriseId
    ) {
        return ResponseEntity.ok(
                projetService.getProjetsEntreprise(entrepriseId)
        );
    }
}
