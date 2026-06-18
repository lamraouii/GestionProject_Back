package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.CreateProjetRequestDto;
import com.ensao.gestionprojet.dto.InviteMembreProjetRequestDto;
import com.ensao.gestionprojet.dto.ProjetResponseDto;

import java.util.List;

public interface ProjetService {

    /** US06 & US07 — Créer un projet personnel ou d'entreprise */
    ProjetResponseDto creerProjet(CreateProjetRequestDto request);

    /** US08 — Valider un projet d'entreprise (ADMIN) */
    ProjetResponseDto validerProjet(Long projetId);

    /** US08 — Rejeter un projet d'entreprise (ADMIN) */
    ProjetResponseDto rejeterProjet(Long projetId);

    /** Récupérer les détails d'un projet */
    ProjetResponseDto getProjet(Long projetId);

    /** Récupérer tous les projets de l'utilisateur courant */
    List<ProjetResponseDto> getMesProjets();

    /** US09 — Inviter un membre au projet (MANAGER) */
    void inviterMembreProjet(Long projetId, InviteMembreProjetRequestDto request);
}
