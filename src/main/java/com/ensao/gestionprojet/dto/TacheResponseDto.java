package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TacheResponseDto {

    private Long id;
    private String titre;
    private String description;
    private String statut;
    private String priorite;
    private Long projetId;
    private Long sprintId;
    private Long utilisateurAssigneId;
    private String utilisateurAssigneNom;
    private Integer storyPoints;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebutTravail;
    private LocalDateTime dateCompletion;
}
