package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    boolean existsByNom(String nom);
}
