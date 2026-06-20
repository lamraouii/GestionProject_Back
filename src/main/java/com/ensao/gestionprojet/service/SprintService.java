package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.*;

import java.util.List;

public interface SprintService {

    /**  — Créer un sprint (MANAGER) */
    SprintResponseDto creerSprint(CreateSprintRequestDto request);

    /**  — Ajouter des tâches au sprint (MANAGER) */
    SprintResponseDto ajouterTaches(Long sprintId, AddTachesSprintRequestDto request);

    /**  — Définir la disponibilité des membres (MANAGER) */
    void definirDisponibilite(Long sprintId, List<DisponibiliteMembreRequestDto> request);

    SprintResponseDto getSprintById(Long sprintId);

    List<DisponibiliteMembreResponseDto> getDisponibilites(Long sprintId);

    /** Récupérer tous les sprints d'un projet */
    List<SprintResponseDto> getSprintsProjet(Long projetId);

    void activerSprint(Long sprintId);

    SprintResponseDto getSprintActif(Long projetId);

    void cloturerSprint(Long sprintId);

    List<BurndownDto> getBurndownChart(Long sprintId);

    List<VelocityDto> getHistoriqueVelocity(Long projetId);
}
