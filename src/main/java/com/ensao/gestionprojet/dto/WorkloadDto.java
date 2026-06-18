package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkloadDto {
    private Long utilisateurId;
    private String utilisateurNom;
    private int tachesTodo;
    private int tachesInProgress;
    private int tachesDone;
    private int totalTaches;
}
