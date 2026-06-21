package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.LoginRequestDto;
import com.ensao.gestionprojet.dto.LoginResponseDto;
import com.ensao.gestionprojet.dto.RegisterRequestDto;
import com.ensao.gestionprojet.dto.RegisterResponseDto;
import com.ensao.gestionprojet.dto.ResendConfirmationRequestDto;
import com.ensao.gestionprojet.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurService utilisateurService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto request) { // <-- @RequestBody EST OBLIGATOIRE
        return ResponseEntity.ok(utilisateurService.register(request));
    }

    @GetMapping("/confirm")
    public String confirmAccount(
            @RequestParam String token
    ) {
        return utilisateurService
                .confirmToken(token);
    }

    @PostMapping("/login")
    public LoginResponseDto login(
            @RequestBody LoginRequestDto request
    ) {
        return utilisateurService.login(request);
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<RegisterResponseDto> resendConfirmation(
            @RequestBody ResendConfirmationRequestDto request
    ) {
        return ResponseEntity.ok(utilisateurService.resendConfirmationEmail(request));
    }

}

