package com.ensao.gestionprojet.controller;

import com.ensao.gestionprojet.dto.BurndownDto;
import com.ensao.gestionprojet.dto.MetriquesDto;
import com.ensao.gestionprojet.dto.WorkloadDto;
import com.ensao.gestionprojet.service.DashboardService;
import com.ensao.gestionprojet.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Obtenir le workload (nombre de taches par statut) par membre du projet
    @GetMapping("/{projetId}/workload")
    public ResponseEntity<List<WorkloadDto>> getWorkload(
            @PathVariable Long projetId
    ) {
        List<WorkloadDto> response = dashboardService.getWorkload(projetId);
        return ResponseEntity.ok(response);
    }

    // Obtenir les métriques Lead Time & Cycle Time moyens pour les tâches complétées du projet
    @GetMapping("/{projetId}/metriques")
    public ResponseEntity<MetriquesDto> getMetriques(
            @PathVariable Long projetId
    ) {
        MetriquesDto response = dashboardService.getMetriques(projetId);
        return ResponseEntity.ok(response);
    }
}
