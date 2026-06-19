package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.MembreProjet;
import com.ensao.gestionprojet.enums.RoleProjet;
import com.ensao.gestionprojet.enums.StatutInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembreProjetRepository extends JpaRepository<MembreProjet, Long> {

    Optional<MembreProjet> findByUtilisateurIdAndProjetId(Long utilisateurId, Long projetId);

    List<MembreProjet> findByUtilisateurIdAndProjetEntrepriseId(
            Long utilisateurId,
            Long entrepriseId
    );

    List<MembreProjet> findByProjetIdAndStatut(Long projetId, StatutInvitation statut);

    List<MembreProjet> findByUtilisateurIdAndStatut(Long utilisateurId, StatutInvitation statut);

    Optional<MembreProjet> findByUtilisateurIdAndProjetIdAndStatut(
            Long utilisateurId, Long projetId, StatutInvitation statut);

    Optional<MembreProjet> findByUtilisateurIdAndProjetIdAndRole(
            Long utilisateurId, Long projetId, RoleProjet role);
}
