package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddTachesSprintRequestDto {

    @NotEmpty(message = "La liste des identifiants des tâches ne peut pas être vide")
    private List<Long> tacheIds;
}
