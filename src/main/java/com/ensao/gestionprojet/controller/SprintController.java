package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.AddTachesSprintRequestDto;
import com.ensao.gestionprojet.dto.CreateSprintRequestDto;
import com.ensao.gestionprojet.dto.DisponibiliteMembreRequestDto;
import com.ensao.gestionprojet.dto.SprintResponseDto;
import com.ensao.gestionprojet.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    /**
     * US13 — Créer un sprint (MANAGER)
     */
    @PostMapping
    public ResponseEntity<SprintResponseDto> creerSprint(
            @Valid @RequestBody CreateSprintRequestDto request
    ) {
        SprintResponseDto response = sprintService.creerSprint(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * US14 — Ajouter des tâches au sprint (MANAGER)
     */
    @PostMapping("/{id}/taches")
    public ResponseEntity<SprintResponseDto> ajouterTaches(
            @PathVariable Long id,
            @Valid @RequestBody AddTachesSprintRequestDto request
    ) {
        SprintResponseDto response = sprintService.ajouterTaches(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * US15 — Définir la disponibilité des membres (MANAGER)
     */
    @PostMapping("/{id}/disponibilites")
    public ResponseEntity<Void> definirDisponibilite(
            @PathVariable Long id,
            @Valid @RequestBody List<DisponibiliteMembreRequestDto> request
    ) {
        sprintService.definirDisponibilite(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Récupérer tous les sprints d'un projet
     */
    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<SprintResponseDto>> getSprintsProjet(
            @PathVariable Long projetId
    ) {
        List<SprintResponseDto> response = sprintService.getSprintsProjet(projetId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sprintId}/activate")
    public ResponseEntity<String> activerSprint(
            @PathVariable Long sprintId
    ) {

        sprintService.activerSprint(
                sprintId
        );

        return ResponseEntity.ok(
                "Sprint activé avec succès"
        );
    }
    
}
