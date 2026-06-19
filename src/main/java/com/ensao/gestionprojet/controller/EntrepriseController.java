package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.CreateEntrepriseRequestDto;
import com.ensao.gestionprojet.dto.EntrepriseResponseDto;
import com.ensao.gestionprojet.service.EntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/entreprises")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    @PostMapping
    public ResponseEntity<EntrepriseResponseDto> creeEntreprise(
            @Valid @RequestBody CreateEntrepriseRequestDto request
    ){
        EntrepriseResponseDto response = entrepriseService.creerEntreprise(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/{entrepriseId}/members/{userId}")
    public ResponseEntity<Void> retirerMembre(
            @PathVariable Long entrepriseId,
            @PathVariable Long userId
    ) {

        entrepriseService.retirerMembre(
                entrepriseId,
                userId
        );

        return ResponseEntity.ok().build();
    }

}
