package com.ensao.gestionprojet.security;

import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.repository.UtilisateurRepo;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UtilisateurRepo utilisateurRepo;

    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        Utilisateur utilisateur =
                utilisateurRepo
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "Utilisateur introuvable"
                                )
                        );

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .disabled(!utilisateur.getEstActif())
                .build();
    }
}