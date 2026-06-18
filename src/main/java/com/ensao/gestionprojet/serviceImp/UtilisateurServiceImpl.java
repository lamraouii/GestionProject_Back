package com.ensao.gestionprojet.serviceImp;

import com.ensao.gestionprojet.dto.LoginRequestDto;
import com.ensao.gestionprojet.dto.LoginResponseDto;
import com.ensao.gestionprojet.dto.RegisterRequestDto;
import com.ensao.gestionprojet.dto.RegisterResponseDto;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.exception.EmailAlreadyExistsException;
import com.ensao.gestionprojet.exception.PasswordsDoNotMatchException;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import com.ensao.gestionprojet.security.JwtService;
import com.ensao.gestionprojet.security.JwtService;
import com.ensao.gestionprojet.service.EmailService;
import com.ensao.gestionprojet.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.ensao.gestionprojet.entity.ConfirmationToken;
import com.ensao.gestionprojet.repository.ConfirmationTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepo utilisateurRepo;
    private final PasswordEncoder passwordEncoder;

    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;

    private final JwtService jwtService;

    // registration
    public RegisterResponseDto register(@RequestBody RegisterRequestDto request){

        // Chek wach kayen mail dejaa
        if (utilisateurRepo.existsUtilisateurByEmail(
                request.getEmail()
        )){
            throw new EmailAlreadyExistsException(
                    "Email déjà utilisé"
            );
        }

        // check wach psswrd mkhtalef 3la dual lconfirmation
        if(request.getMotDePasse()==null || !request.getMotDePasse().equals(request.getConfirmationMotdePasse())){
            throw new PasswordsDoNotMatchException(
                    "Les mots de passe ne correspondent pas"

            );
        }

        // hashing pswrd
        String hashedPasswrd = passwordEncoder.encode(request.getMotDePasse());

        Utilisateur utilisateur =  Utilisateur.builder()
                .email(request.getEmail())
                .motDePasse(hashedPasswrd)
                .prenom(request.getPrenom())
                .nom(request.getNom())
                .build();

        Utilisateur savedUser = utilisateurRepo.save(utilisateur);

        // token and email sending
        String token = UUID.randomUUID().toString();

        ConfirmationToken confirmationToken =
                ConfirmationToken.builder()
                        .token(token)
                        .expirationDate(
                                LocalDateTime.now().plusHours(24)
                        )
                        .utilisateur(savedUser)
                        .build();

        confirmationTokenRepository.save(
                confirmationToken
        );

        String confirmationLink =
                "http://localhost:8080/api/auth/confirm?token="
                        + token;

        emailService.envoyerEmailConfirmation(
                savedUser.getEmail(),
                confirmationLink
        );


        return  RegisterResponseDto.builder()
                .id(savedUser.getId())
                .nom(savedUser.getNom())
                .prenom(savedUser.getPrenom())
                .email(savedUser.getEmail())
                .message("Compte créé avec succès")
                .build();
    }

    @Override
    public String confirmToken(String token) {

        ConfirmationToken confirmationToken =
                confirmationTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Token invalide"
                                )
                        );

        if(confirmationToken.getUtilise()) {
            throw new RuntimeException(
                    "Token déjà utilisé"
            );
        }

        if(confirmationToken.getExpirationDate()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Token expiré"
            );
        }

        Utilisateur utilisateur =
                confirmationToken.getUtilisateur();

        utilisateur.setEstActif(true);

        utilisateurRepo.save(utilisateur);

        confirmationToken.setUtilise(true);

        confirmationTokenRepository
                .save(confirmationToken);

        return "Compte confirmé avec succès";
    }

    // login

    @Override
    public LoginResponseDto login(
            LoginRequestDto request
    ) {

        Utilisateur utilisateur =
                utilisateurRepo
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Email incorrect"
                                )
                        );

        if(!utilisateur.getEstActif()) {

            throw new RuntimeException(
                    "Compte non confirmé"
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getMotDePasse(),
                        utilisateur.getMotDePasse()
                );

        if(!passwordMatches) {

            throw new RuntimeException(
                    "Mot de passe incorrect"
            );
        }

        String token =
                jwtService.generateToken(
                        utilisateur.getEmail()
                );

        return LoginResponseDto.builder()
                .token(token)
                .message("Connexion réussie")
                .build();
    }

}
