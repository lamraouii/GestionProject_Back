package com.ensao.gestionprojet.helpers;

import com.ensao.gestionprojet.enums.RoleProjet;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import lombok.AllArgsConstructor;



@AllArgsConstructor
public class VerifyManager {

    private final MembreProjetRepository membreProjetRepository;

    public void verifierManager(Long utilisateurId, Long projetId) {
        membreProjetRepository.findByUtilisateurIdAndProjetIdAndRole(utilisateurId, projetId, RoleProjet.MANAGER)
                .orElseThrow(() -> new RuntimeException("Seul le Manager du projet peut effectuer cette action"));
    }
}
