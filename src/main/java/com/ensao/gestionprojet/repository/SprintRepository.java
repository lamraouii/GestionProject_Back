package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.Sprint;
import com.ensao.gestionprojet.enums.StatutSprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjetId(Long projetId);

    Optional<Sprint> findByProjetIdAndStatut(Long projetId, StatutSprint statut);

    boolean existsByProjetIdAndStatut(Long projetId, StatutSprint statut);
}
