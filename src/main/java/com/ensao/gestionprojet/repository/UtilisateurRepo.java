package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepo extends JpaRepository<Utilisateur, Long> {

    boolean existsUtilisateurByEmail(String email);
    Optional<Utilisateur> findByEmail(String email);

}
