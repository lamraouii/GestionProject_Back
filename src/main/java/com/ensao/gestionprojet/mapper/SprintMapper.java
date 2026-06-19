package com.ensao.gestionprojet.mapper;

import com.ensao.gestionprojet.dto.*;
import com.ensao.gestionprojet.entity.Sprint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SprintMapper {

    public SprintResponseDto toDto(Sprint sprint) {

        return SprintResponseDto.builder()
                .id(sprint.getId())
                .nom(sprint.getNom())
                .objectif(sprint.getObjectif())
                .dateDebut(sprint.getDateDebut())
                .dateFin(sprint.getDateFin())
                .statut(sprint.getStatut().name())
                .projetId(sprint.getProjet().getId())
                .velocite(sprint.getVelocite())
                .dateCreation(sprint.getDateCreation())
                .build();
    }

    public SprintResponseDto toDtoWithTasks(Sprint sprint, List<TacheResponseDto> taches) {

        SprintResponseDto dto = toDto(sprint);
        dto.setProjetNom(sprint.getProjet().getNom());
        dto.setTaches(taches);

        return dto;
    }
}