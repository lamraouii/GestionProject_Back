package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.LoginRequestDto;
import com.ensao.gestionprojet.dto.LoginResponseDto;
import com.ensao.gestionprojet.dto.RegisterRequestDto;
import com.ensao.gestionprojet.dto.RegisterResponseDto;
import com.ensao.gestionprojet.dto.ResendConfirmationRequestDto;
import com.ensao.gestionprojet.entity.Utilisateur;

import java.util.List;


public interface UtilisateurService {

    RegisterResponseDto register(RegisterRequestDto request);

    String confirmToken(String token);

    LoginResponseDto login(
            LoginRequestDto request
    );

    RegisterResponseDto resendConfirmationEmail(ResendConfirmationRequestDto request);

}
