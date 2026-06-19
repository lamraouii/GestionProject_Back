package com.ensao.gestionprojet.dto;

import java.time.LocalDate;

public record BurndownDto(
        LocalDate date,
        Integer remainingPoints
) {
}
