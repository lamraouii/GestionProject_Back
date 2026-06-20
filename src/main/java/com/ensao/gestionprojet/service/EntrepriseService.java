package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.CreateEntrepriseRequestDto;
import com.ensao.gestionprojet.dto.EntrepriseResponseDto;
import com.ensao.gestionprojet.dto.MemberResponseDto;

import java.util.List;

public interface EntrepriseService {
    EntrepriseResponseDto creerEntreprise(CreateEntrepriseRequestDto request);

    List<EntrepriseResponseDto> getMesEntreprises();

    EntrepriseResponseDto getEntreprise(Long entrepriseId);

    List<MemberResponseDto> getMembres(Long entrepriseId);

    void retirerMembre(
            Long entrepriseId,
            Long utilisateurId
    );
}
