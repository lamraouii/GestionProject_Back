package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.MembreEntreprise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembreEntrepriseRepository extends JpaRepository<MembreEntreprise, Long> {

    Optional<MembreEntreprise> findByUtilisateurIdAndEntrepriseId(Long userId, Long entrepriseId);

}
