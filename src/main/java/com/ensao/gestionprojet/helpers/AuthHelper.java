package com.ensao.gestionprojet.helpers;

import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.repository.UtilisateurRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilitaire centralisant la récupération de l'utilisateur authentifié
 * depuis le SecurityContext.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UtilisateurRepo utilisateurRepo;

    /**
     * Récupère l'utilisateur actuellement authentifié.
     * @return L'entité Utilisateur correspondant au token JWT courant
     * @throws RuntimeException si l'utilisateur n'est pas trouvé
     */
    public Utilisateur getUtilisateurCourant() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = authentication.getName();

        return utilisateurRepo
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}
