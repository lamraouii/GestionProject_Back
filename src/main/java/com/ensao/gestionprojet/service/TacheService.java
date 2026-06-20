package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.CreateTacheRequestDto;
import com.ensao.gestionprojet.dto.TacheResponseDto;
import com.ensao.gestionprojet.dto.UpdateStatutTacheRequestDto;

import com.ensao.gestionprojet.dto.KanbanBoardDto;

import java.util.List;

public interface TacheService {

    TacheResponseDto creerTache(CreateTacheRequestDto request);

    TacheResponseDto assignerTache(Long tacheId, Long utilisateurId);

    TacheResponseDto mettreAJourStatut(Long tacheId, UpdateStatutTacheRequestDto request);

    List<TacheResponseDto> getTachesProjet(Long projetId);

    List<TacheResponseDto> getTachesBacklog(Long projetId);

    /**  Obtenir la vue Kanban Board */
    KanbanBoardDto getKanbanBoard(Long projetId);
}
