package com.ensao.gestionprojet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String confirmationMotdePasse;
}
