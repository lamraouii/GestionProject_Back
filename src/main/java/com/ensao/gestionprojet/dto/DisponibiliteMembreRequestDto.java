package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisponibiliteMembreRequestDto {

    @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
    private Long utilisateurId;

    @NotNull(message = "Les heures disponibles sont obligatoires")
    @Min(value = 0, message = "Les heures disponibles ne peuvent pas être négatives")
    private Integer heuresDisponibles;
}
