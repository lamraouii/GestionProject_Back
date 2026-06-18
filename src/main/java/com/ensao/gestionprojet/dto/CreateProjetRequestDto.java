package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateProjetRequestDto {

    @NotBlank(message = "Le nom du projet est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le type de projet est obligatoire (PERSONAL ou ENTREPRISE)")
    private String type;        // "PERSONAL" ou "ENTREPRISE"

    private Long entrepriseId;  // obligatoire si type = ENTREPRISE, null sinon

    private LocalDate dateDebut;

    private LocalDate dateFin;
}
