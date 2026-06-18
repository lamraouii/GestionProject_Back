package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.CreateTacheRequestDto;
import com.ensao.gestionprojet.dto.TacheResponseDto;
import com.ensao.gestionprojet.dto.UpdateStatutTacheRequestDto;
import com.ensao.gestionprojet.service.TacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taches")
@RequiredArgsConstructor
public class TacheController {

    private final TacheService tacheService;

    /**
     * US10 — Créer une tâche (MANAGER)
     */
    @PostMapping
    public ResponseEntity<TacheResponseDto> creerTache(
            @Valid @RequestBody CreateTacheRequestDto request
    ) {
        TacheResponseDto response = tacheService.creerTache(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * US11 — Assigner une tâche à un membre (MANAGER)
     */
    @PutMapping("/{id}/assigner/{userId}")
    public ResponseEntity<TacheResponseDto> assignerTache(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        TacheResponseDto response = tacheService.assignerTache(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * US12 — Mettre à jour le statut d'une tâche (MANAGER ou membre assigné)
     */
    @PutMapping("/{id}/statut")
    public ResponseEntity<TacheResponseDto> mettreAJourStatut(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatutTacheRequestDto request
    ) {
        TacheResponseDto response = tacheService.mettreAJourStatut(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer toutes les tâches d'un projet
     */
    @GetMapping("/projet/{projetId}")
    public ResponseEntity<List<TacheResponseDto>> getTachesProjet(
            @PathVariable Long projetId
    ) {
        List<TacheResponseDto> response = tacheService.getTachesProjet(projetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les tâches du backlog (sprint_id = NULL)
     */
    @GetMapping("/backlog/{projetId}")
    public ResponseEntity<List<TacheResponseDto>> getTachesBacklog(
            @PathVariable Long projetId
    ) {
        List<TacheResponseDto> response = tacheService.getTachesBacklog(projetId);
        return ResponseEntity.ok(response);
    }
}
