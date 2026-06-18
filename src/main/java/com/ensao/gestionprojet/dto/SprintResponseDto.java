package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SprintResponseDto {

    private Long id;
    private String nom;
    private String objectif;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private Long projetId;
    private Integer velocite;
    private LocalDateTime dateCreation;
}
