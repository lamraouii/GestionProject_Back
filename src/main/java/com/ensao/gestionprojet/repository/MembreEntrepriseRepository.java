package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.MembreEntreprise;
import com.ensao.gestionprojet.enums.StatutInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembreEntrepriseRepository extends JpaRepository<MembreEntreprise, Long> {

    Optional<MembreEntreprise> findByUtilisateurIdAndEntrepriseId(Long userId, Long entrepriseId);

    List<MembreEntreprise> findByUtilisateurIdAndStatut(Long userId, StatutInvitation statut);

    List<MembreEntreprise> findByEntrepriseIdAndStatut(Long entrepriseId, StatutInvitation statut);

}
