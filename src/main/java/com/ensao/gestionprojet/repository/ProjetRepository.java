package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.Projet;
import com.ensao.gestionprojet.enums.StatutProjet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjetRepository extends JpaRepository<Projet, Long> {

    List<Projet> findByEntrepriseId(Long entrepriseId);

    List<Projet> findByCreateurId(Long createurId);

    List<Projet> findByEntrepriseIdAndStatut(Long entrepriseId, StatutProjet statut);
}
