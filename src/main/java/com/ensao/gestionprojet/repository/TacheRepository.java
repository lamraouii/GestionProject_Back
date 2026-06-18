package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.Tache;
import com.ensao.gestionprojet.enums.StatutTache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TacheRepository extends JpaRepository<Tache, Long> {

    List<Tache> findByProjetId(Long projetId);

    List<Tache> findBySprintId(Long sprintId);

    /** Tâches du backlog (non liées à un sprint) */
    List<Tache> findByProjetIdAndSprintIsNull(Long projetId);

    List<Tache> findByProjetIdAndStatut(Long projetId, StatutTache statut);

    List<Tache> findByUtilisateurAssigneId(Long utilisateurId);

    List<Tache> findByProjetIdAndUtilisateurAssigneId(Long projetId, Long utilisateurId);

    long countBySprintIdAndStatut(Long sprintId, StatutTache statut);

    List<Tache> findBySprintIdAndStatut(Long sprintId, StatutTache statut);
}
