package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EntrepriseResponseDto {

    private Long id;

    private String nom;

    private String description;

    private String urlLogo;

    private String role;
}