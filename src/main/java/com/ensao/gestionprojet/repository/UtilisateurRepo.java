package com.ensao.gestionprojet.repository;

import com.ensao.gestionprojet.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisateurRepo extends JpaRepository<Utilisateur, Long> {

    boolean existsUtilisateurByEmail(String email);

}
