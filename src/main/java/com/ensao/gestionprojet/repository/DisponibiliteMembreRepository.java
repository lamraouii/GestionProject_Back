package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.DisponibiliteMembre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisponibiliteMembreRepository extends JpaRepository<DisponibiliteMembre, Long> {

    List<DisponibiliteMembre> findBySprintId(Long sprintId);

    Optional<DisponibiliteMembre> findBySprintIdAndUtilisateurId(Long sprintId, Long utilisateurId);
}
