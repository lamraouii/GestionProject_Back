package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEntrepriseRequestDto {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nom;

    private String description;

    private String urlLogo;
}
