package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.CreateTacheRequestDto;
import com.ensao.gestionprojet.dto.TacheResponseDto;
import com.ensao.gestionprojet.dto.UpdateStatutTacheRequestDto;

import java.util.List;

public interface TacheService {

    /** US10 — Créer une tâche (MANAGER) */
    TacheResponseDto creerTache(CreateTacheRequestDto request);

    /** US11 — Assigner une tâche à un membre (MANAGER) */
    TacheResponseDto assignerTache(Long tacheId, Long utilisateurId);

    /** US12 — Mettre à jour le statut d'une tâche (MANAGER ou membre assigné) */
    TacheResponseDto mettreAJourStatut(Long tacheId, UpdateStatutTacheRequestDto request);

    /** Récupérer toutes les tâches d'un projet */
    List<TacheResponseDto> getTachesProjet(Long projetId);

    /** Récupérer les tâches du backlog (sprint_id = NULL) */
    List<TacheResponseDto> getTachesBacklog(Long projetId);
}
