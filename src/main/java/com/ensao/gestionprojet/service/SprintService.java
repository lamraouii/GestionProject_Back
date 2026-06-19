package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.AddTachesSprintRequestDto;
import com.ensao.gestionprojet.dto.CreateSprintRequestDto;
import com.ensao.gestionprojet.dto.DisponibiliteMembreRequestDto;
import com.ensao.gestionprojet.dto.SprintResponseDto;

import java.util.List;

public interface SprintService {

    /** US13 — Créer un sprint (MANAGER) */
    SprintResponseDto creerSprint(CreateSprintRequestDto request);

    /** US14 — Ajouter des tâches au sprint (MANAGER) */
    SprintResponseDto ajouterTaches(Long sprintId, AddTachesSprintRequestDto request);

    /** US15 — Définir la disponibilité des membres (MANAGER) */
    void definirDisponibilite(Long sprintId, List<DisponibiliteMembreRequestDto> request);

    /** Récupérer tous les sprints d'un projet */
    List<SprintResponseDto> getSprintsProjet(Long projetId);

    void activerSprint(Long sprintId);

}
