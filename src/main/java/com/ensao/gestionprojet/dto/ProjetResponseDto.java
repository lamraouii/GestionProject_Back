package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProjetResponseDto {

    private Long id;
    private String nom;
    private String description;
    private String type;
    private String statut;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long createurId;
    private String createurNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDateTime dateCreation;
}
