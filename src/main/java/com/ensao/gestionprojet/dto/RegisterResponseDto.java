package com.ensao.gestionprojet.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterResponseDto {

    private Long id ;
    private String nom;
    private String prenom;
    private String email;
    private String message;

}
