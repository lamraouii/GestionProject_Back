package com.ensao.gestionprojet.helpers;

import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.entity.Utilisateur;
import com.ensao.gestionprojet.enums.RoleEntreprise;
import com.ensao.gestionprojet.enums.RoleProjet;
import com.ensao.gestionprojet.enums.StatutInvitation;
import com.ensao.gestionprojet.enums.TypeProjet;
import com.ensao.gestionprojet.repository.MembreEntrepriseRepository;
import com.ensao.gestionprojet.repository.MembreProjetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectAccessHelper {

    private final MembreProjetRepository membreProjetRepository;
    private final MembreEntrepriseRepository membreEntrepriseRepository;

    public Optional<RoleProjet> resolveRole(Utilisateur utilisateur, Projet projet) {
        return membreProjetRepository
                .findByUtilisateurIdAndProjetIdAndStatut(
                        utilisateur.getId(),
                        projet.getId(),
                        StatutInvitation.ACCEPTED
                )
                .map(membreProjet -> Optional.of(membreProjet.getRole()))
                .orElseGet(() -> resolveEntrepriseAdminRole(utilisateur, projet));
    }

    public RoleProjet requireAccess(Utilisateur utilisateur, Projet projet) {
        return resolveRole(utilisateur, projet)
                .orElseThrow(() -> new RuntimeException("Acces refuse - vous n'avez pas acces a ce projet"));
    }

    public void requireManager(Utilisateur utilisateur, Projet projet) {
        RoleProjet role = requireAccess(utilisateur, projet);

        if (role != RoleProjet.MANAGER) {
            throw new RuntimeException("Seul le Manager du projet peut effectuer cette action");
        }
    }

    private Optional<RoleProjet> resolveEntrepriseAdminRole(Utilisateur utilisateur, Projet projet) {
        if (projet.getType() != TypeProjet.ENTREPRISE || projet.getEntreprise() == null) {
            return Optional.empty();
        }

        return membreEntrepriseRepository
                .findByUtilisateurIdAndEntrepriseId(
                        utilisateur.getId(),
                        projet.getEntreprise().getId()
                )
                .filter(membreEntreprise ->
                        membreEntreprise.getStatut() == StatutInvitation.ACCEPTED
                                && membreEntreprise.getRole() == RoleEntreprise.ADMIN
                )
                .map(membreEntreprise -> RoleProjet.MANAGER);
    }
}
