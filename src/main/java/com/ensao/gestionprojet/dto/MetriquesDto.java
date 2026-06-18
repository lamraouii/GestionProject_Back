package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MetriquesDto {
    private Double leadTimeMoyenHeures;
    private Double cycleTimeMoyenHeures;
}
