package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.CreateEntrepriseRequestDto;
import com.ensao.gestionprojet.dto.EntrepriseResponseDto;

public interface EntrepriseService {
    EntrepriseResponseDto creerEntreprise(CreateEntrepriseRequestDto request);

    void retirerMembre(
            Long entrepriseId,
            Long utilisateurId
    );
}
