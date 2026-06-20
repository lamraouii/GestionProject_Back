package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.CreateEntrepriseRequestDto;
import com.ensao.gestionprojet.dto.EntrepriseResponseDto;
import com.ensao.gestionprojet.dto.MemberResponseDto;
import com.ensao.gestionprojet.service.EntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/mes-entreprises")
    public ResponseEntity<List<EntrepriseResponseDto>> getMesEntreprises() {
        return ResponseEntity.ok(
                entrepriseService.getMesEntreprises()
        );
    }

    @GetMapping("/{entrepriseId}")
    public ResponseEntity<EntrepriseResponseDto> getEntreprise(
            @PathVariable Long entrepriseId
    ) {
        return ResponseEntity.ok(
                entrepriseService.getEntreprise(entrepriseId)
        );
    }

    @GetMapping("/{entrepriseId}/members")
    public ResponseEntity<List<MemberResponseDto>> getMembres(
            @PathVariable Long entrepriseId
    ) {
        return ResponseEntity.ok(
                entrepriseService.getMembres(entrepriseId)
        );
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
