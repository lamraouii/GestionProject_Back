package com.ensao.gestionprojet.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisponibiliteMembreResponseDto {

    private Long id;
    private Long sprintId;
    private Long utilisateurId;
    private String utilisateurNom;
    private Integer heuresDisponibles;
}
