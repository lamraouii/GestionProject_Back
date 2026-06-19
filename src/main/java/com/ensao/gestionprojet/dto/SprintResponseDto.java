package com.ensao.gestionprojet.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SprintResponseDto {

    private Long id;
    private String nom;
    private String objectif;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private Long projetId;
    private String projetNom;
    private Integer velocite;
    private List<TacheResponseDto> taches ;
    private LocalDateTime dateCreation;
}
