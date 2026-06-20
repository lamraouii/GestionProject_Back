package com.ensao.gestionprojet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;   // ✅ requis quand on combine Builder + NoArgsConstructor
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;    // ✅ requis par Hibernate

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor                  // ✅ constructeur vide pour Hibernate
@AllArgsConstructor                 // ✅ constructeur complet pour @Builder
public class BurndownSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Sprint sprint;

    private LocalDate date;

    private Integer remainingPoints;
}