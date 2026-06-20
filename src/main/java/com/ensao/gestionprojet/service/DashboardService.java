package com.ensao.gestionprojet.service;

import com.ensao.gestionprojet.dto.BurndownDto;
import com.ensao.gestionprojet.dto.MetriquesDto;
import com.ensao.gestionprojet.dto.WorkloadDto;

import java.util.List;

public interface DashboardService {

    // Obtenir le workload des membres du projet
    List<WorkloadDto> getWorkload(Long projetId);

    // Obtenir les métriques (Lead/Cycle Time)
    MetriquesDto getMetriques(Long projetId);


}
