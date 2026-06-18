package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTacheRequestDto {

    @NotBlank(message = "Le titre de la tâche est obligatoire")
    private String titre;

    private String description;

    private String priorite;    // "LOW", "MEDIUM", "HIGH", "CRITICAL"

    private Long utilisateurAssigneId;  // nullable — peut être assigné plus tard

    @NotNull(message = "L'identifiant du projet est obligatoire")
    private Long projetId;

    private Long sprintId;      // nullable — null = backlog

    private Integer storyPoints;
}
