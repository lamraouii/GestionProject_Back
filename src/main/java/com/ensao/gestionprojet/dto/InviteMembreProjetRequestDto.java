package com.ensao.gestionprojet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteMembreProjetRequestDto {

    @Email(message = "Email invalide")
    @NotBlank(message = "Email obligatoire")
    private String email;
}
