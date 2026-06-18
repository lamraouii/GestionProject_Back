package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatutTacheRequestDto {

    @NotNull(message = "Le statut est obligatoire")
    private String statut;  // "TODO", "IN_PROGRESS", "DONE"
}
