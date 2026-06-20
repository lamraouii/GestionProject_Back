package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.CreateProjetRequestDto;
import com.ensao.gestionprojet.dto.InviteMembreProjetRequestDto;
import com.ensao.gestionprojet.dto.MemberResponseDto;
import com.ensao.gestionprojet.dto.ProjetResponseDto;

import java.util.List;

public interface ProjetService {

    /** Créer un projet personnel ou d'entreprise */
    ProjetResponseDto creerProjet(CreateProjetRequestDto request);

    /** Valider un projet d'entreprise (ADMIN) */
    ProjetResponseDto validerProjet(Long projetId);

    /** Rejeter un projet d'entreprise (ADMIN) */
    ProjetResponseDto rejeterProjet(Long projetId);

    /** Récupérer les détails d'un projet */
    ProjetResponseDto getProjet(Long projetId);

    /** Récupérer tous les projets de l'utilisateur courant */
    List<ProjetResponseDto> getMesProjets();

    /** Récupérer les projets d'une entreprise pour ses membres acceptés */
    List<ProjetResponseDto> getProjetsEntreprise(Long entrepriseId);

    /** Récupérer les membres acceptés d'un projet */
    List<MemberResponseDto> getMembresProjet(Long projetId);

    /**  Inviter un membre au projet (MANAGER) */
    void inviterMembreProjet(Long projetId, InviteMembreProjetRequestDto request);
}
