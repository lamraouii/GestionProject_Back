package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.*;
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

    // Créer un sprint (MANAGER)
    @PostMapping
    public ResponseEntity<SprintResponseDto> creerSprint(
            @Valid @RequestBody CreateSprintRequestDto request
    ) {
        SprintResponseDto response = sprintService.creerSprint(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Ajouter des tâches au sprint (MANAGER)
    @PostMapping("/{id}/taches")
    public ResponseEntity<SprintResponseDto> ajouterTaches(
            @PathVariable Long id,
            @Valid @RequestBody AddTachesSprintRequestDto request
    ) {
        SprintResponseDto response = sprintService.ajouterTaches(id, request);
        return ResponseEntity.ok(response);
    }

   // Définir la disponibilité des membres (MANAGER)
    @PostMapping("/{id}/disponibilites")
    public ResponseEntity<Void> definirDisponibilite(
            @PathVariable Long id,
            @Valid @RequestBody List<DisponibiliteMembreRequestDto> request
    ) {
        sprintService.definirDisponibilite(id, request);
        return ResponseEntity.ok().build();
    }

    // Récupérer tous les sprints d'un projet
    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintResponseDto> getSprintById(
            @PathVariable Long sprintId
    ) {
        SprintResponseDto response = sprintService.getSprintById(sprintId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sprintId}/disponibilites")
    public ResponseEntity<List<DisponibiliteMembreResponseDto>> getDisponibilites(
            @PathVariable Long sprintId
    ) {
        List<DisponibiliteMembreResponseDto> response =
                sprintService.getDisponibilites(sprintId);

        return ResponseEntity.ok(response);
    }

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

    @PostMapping("/{sprintId}/close")
    public ResponseEntity<String> cloturerSprint(@PathVariable Long sprintId) {

        sprintService.cloturerSprint(sprintId);

        return ResponseEntity.ok("Sprint clôturé avec succès");
    }


    @GetMapping("/{sprintId}/burndown")
    public ResponseEntity<List<BurndownDto>> getBurndown(
            @PathVariable Long sprintId
    ) {
        return ResponseEntity.ok(
                sprintService.getBurndownChart(sprintId)
        );
    }

    @GetMapping("/projets/{projetId}/velocity")
    public ResponseEntity<List<VelocityDto>>
    getHistoriqueVelocity(
            @PathVariable Long projetId
    ) {

        return ResponseEntity.ok(
                sprintService.getHistoriqueVelocity(
                        projetId
                )
        );
    }

}
